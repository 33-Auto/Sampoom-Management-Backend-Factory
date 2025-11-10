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

                // MPS 이벤트 발행 상세 로깅 추가
                if (o.getEventType().startsWith("Mps")) {
                    log.info("MPS 이벤트 발행 시작 - OUTBOX ID: {}, EventType: {}, AggregateId: {}",
                        o.getId(), o.getEventType(), o.getAggregateId());
                }

                // eventType에 따라 적절한 이벤트 타입과 토픽 결정
                // PartOrder 관련 이벤트 (일반 PartOrder + MPS)
                if (o.getEventType().startsWith("PartOrder") || o.getEventType().startsWith("Mps")) {
                    PartOrderEvent partOrderEvent = objectMapper.treeToValue(o.getPayload(), PartOrderEvent.class);
                    String eventJson = objectMapper.writeValueAsString(partOrderEvent);
                    topic = TOPIC_PART_ORDER;
                    evt = eventJson;

                    // MPS 이벤트인 경우 추가 로깅
                    if (o.getEventType().startsWith("Mps")) {
                        log.info("MPS 이벤트 JSON 변환 완료 - EventType: {}, Topic: {}, EventJson 길이: {}",
                            o.getEventType(), topic, eventJson.length());
                    }
                } else {
                    FactoryEvent factoryEvent = objectMapper.treeToValue(o.getPayload(), FactoryEvent.class);
                    String eventJson = objectMapper.writeValueAsString(factoryEvent);
                    topic = TOPIC_FACTORY;
                    evt = eventJson;
                }

                // MPS 이벤트 Kafka 전송 로깅
                if (o.getEventType().startsWith("Mps")) {
                    log.info("MPS 이벤트 Kafka 전송 시작 - EventType: {}, Topic: {}, Key: {}",
                        o.getEventType(), topic, String.valueOf(o.getAggregateId()));
                }

                kafkaTemplate.send(topic, String.valueOf(o.getAggregateId()), evt)
                                                     .get(5, TimeUnit.SECONDS);

                o.markPublished();

                // MPS 이벤트 발행 완료 로깅
                if (o.getEventType().startsWith("Mps")) {
                    log.info("MPS 이벤트 Kafka 전송 완료 - OUTBOX ID: {}, EventType: {}, 상태: published",
                        o.getId(), o.getEventType());
                }

            } catch (Exception e){
                // MPS 이벤트 발행 실패 로깅
                if (o.getEventType().startsWith("Mps")) {
                    log.error("MPS 이벤트 Kafka 전송 실패 - OUTBOX ID: {}, EventType: {}, 오류: {}",
                        o.getId(), o.getEventType(), e.getMessage(), e);
                }

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