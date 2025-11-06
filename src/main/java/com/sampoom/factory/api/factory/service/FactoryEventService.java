package com.sampoom.factory.api.factory.service;

//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.sampoom.factory.api.factory.event.FactoryEvent;
//import com.sampoom.factory.api.factory.outbox.FactoryOutbox;
//import com.sampoom.factory.api.factory.outbox.FactoryOutboxRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.OffsetDateTime;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class FactoryEventService {
//
//    private final FactoryOutboxRepository outboxRepository;
//    private final ObjectMapper objectMapper;
//
//    @Transactional
//    public void recordFactoryCreated(Factory factory){
//        enqueueEvent("FactoryCreated", factory, nvl(factory.getVersion(), 0L  ), false);
//    }
//
//    @Transactional
//    public void recordFactoryUpdated(Factory factory){
//        enqueueEvent("FactoryUpdated", factory, nvl(factory.getVersion(), 0L ), false);
//    }
//
//    @Transactional
//    public void recordFactoryDeleted(Factory factory){
//
//        enqueueEvent("FactoryDeleted", factory, nvl(factory.getVersion(), 0L), true);
//    }
//
//    // ===== 공통 헬퍼 =====
//    private void enqueueEvent(String eventType, Factory factory, long version, Boolean deleted) {
//        FactoryEvent evt = new FactoryEvent(
//                UUID.randomUUID().toString(),
//                eventType,
//                version,
//                OffsetDateTime.now().toString(),
//                new FactoryEvent.Payload(
//                        factory.getId(),
//                        factory.getName(),
//                        factory.getAddress(),
//                        factory.getStatus().name(),
//                        deleted
//                )
//        );
//
//        try {
//            JsonNode payload = objectMapper.valueToTree(evt);
//            outboxRepository.save(
//                    FactoryOutbox.ready(
//                            factory.getId(),
//                            eventType,
//                            UUID.fromString(evt.eventId()),
//                            payload
//                    )
//            );
//        } catch (Exception e) {
//            throw new IllegalStateException("Serialize " + eventType + " event failed", e);
//        }
//    }
//
//    private long nvl(Long v, long def) { return v == null ? def : v; }
//}