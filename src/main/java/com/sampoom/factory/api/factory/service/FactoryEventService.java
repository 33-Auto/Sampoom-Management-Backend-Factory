package com.sampoom.factory.api.factory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.event.FactoryEvent;
import com.sampoom.factory.api.factory.outbox.FactoryOutbox;
import com.sampoom.factory.api.factory.outbox.FactoryOutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactoryEventService {

    private final FactoryOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordFactoryCreated(Factory factory){
        enqueueEvent("FactoryCreated", factory, nvl(factory.getVersion(), 0L));
    }

    @Transactional
    public void recordFactoryUpdated(Factory factory){
        enqueueEvent("FactoryUpdated", factory, nvl(factory.getVersion(), 0L));
    }

    @Transactional
    public void recordFactoryDeleted(Factory factory){
        // ✅ 삭제는 반드시 current+1 로 보냄
        long nextVersion = nvl(factory.getVersion(), 0L) + 1;
        enqueueEvent("FactoryDeleted", factory, nextVersion);
    }

    // ===== 공통 헬퍼 (버전 명시) =====
    private void enqueueEvent(String eventType, Factory factory, long version) {
        FactoryEvent evt = new FactoryEvent(
                UUID.randomUUID().toString(),
                eventType,
                version,                                // ✅ 여기서 명시적으로 사용
                OffsetDateTime.now().toString(),
                new FactoryEvent.Payload(
                        factory.getId(),
                        factory.getName(),
                        factory.getAddress(),
                        factory.getStatus().name()
                )
        );

        try {
            JsonNode payload = objectMapper.valueToTree(evt);
            outboxRepository.save(
                    FactoryOutbox.ready(
                            factory.getId(),
                            eventType,
                            UUID.fromString(evt.eventId()),
                            payload
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException("Serialize " + eventType + " event failed", e);
        }
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}