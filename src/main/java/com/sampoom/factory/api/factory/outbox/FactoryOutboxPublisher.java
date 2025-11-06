package com.sampoom.factory.api.factory.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.factory.event.FactoryEvent;
import com.sampoom.factory.api.part.event.PartOrderEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FactoryOutboxPublisher {
    private final FactoryOutboxRepository repo;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_FACTORY = "factory-events";
    private static final String TOPIC_PART_ORDER = "part-order-events";
    private static final int BATCH = 100;
    private static final int MAX_RETRY = 10;
    private static final long BASE_BACKOFF_MS = 500;     // 0.5s
    private static final long MAX_BACKOFF_MS  = 60_000;  // 60s

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishBatch(){
        List<FactoryOutbox> batch = repo.pickReadyBatch(BATCH, MAX_RETRY);
        if (batch.isEmpty()) return;

        for (FactoryOutbox o : batch){
            try {
                Object evt;
                String topic;

                // eventType에 따라 적절한 이벤트 타입과 토픽 결정
                if (o.getEventType().startsWith("PartOrder")) {
                    PartOrderEvent partOrderEvent = objectMapper.treeToValue(o.getPayload(), PartOrderEvent.class);
                    String eventJson = objectMapper.writeValueAsString(partOrderEvent);
                    topic = TOPIC_PART_ORDER;
                    evt = eventJson;
                } else {
                    FactoryEvent factoryEvent = objectMapper.treeToValue(o.getPayload(), FactoryEvent.class);
                    String eventJson = objectMapper.writeValueAsString(factoryEvent);
                    topic = TOPIC_FACTORY;
                    evt = eventJson;
                }

                kafkaTemplate.send(topic, String.valueOf(o.getAggregateId()), evt)
                                                     .get(5, TimeUnit.SECONDS);

                o.markPublished();

            } catch (Exception e){
                int nextRetry = o.getRetryCount() + 1;


                if (nextRetry >= MAX_RETRY) {
                    o.markDead(shorten(e.getMessage(), 2000));
                    log.error("Outbox DEAD id={} retry={} cause={}", o.getId(), o.getRetryCount(), e.toString());
                    continue;
                }

                long backoffMs = computeBackoffMs(nextRetry);
                LocalDateTime next = LocalDateTime.now().plusNanos(backoffMs * 1_000_000);
                o.markFailed(shorten(e.getMessage(), 2000), next);
                log.warn("Outbox publish failed id={} retry={} cause={}", o.getId(), o.getRetryCount(), e.toString());
            }
        }
    }

    private String shorten(String s, int max){ return (s==null||s.length()<=max) ? s : s.substring(0,max); }

    private long computeBackoffMs(int retry) {
        double exp = Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * Math.pow(2, Math.max(0, retry - 1)));
        double jitter = exp * (Math.random() * 0.1); // 0~10% 지터
        return (long) Math.min(MAX_BACKOFF_MS, exp + jitter);
    }
}