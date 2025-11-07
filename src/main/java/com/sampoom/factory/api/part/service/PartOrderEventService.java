package com.sampoom.factory.api.part.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.factory.outbox.FactoryOutbox;
import com.sampoom.factory.api.factory.outbox.FactoryOutboxRepository;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.event.PartOrderEvent;
import com.sampoom.factory.api.part.repository.PartProjectionRepository;
import com.sampoom.factory.api.factory.repository.FactoryProjectionRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartOrderEventService {

    private final FactoryOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PartProjectionRepository partProjectionRepository;
    private final FactoryProjectionRepository factoryProjectionRepository;

    @Transactional
    public void recordPartOrderCreated(PartOrder partOrder) {
        log.info("부품 주문 생성 이벤트 발행 - 주문 ID: {}, 주문 코드: {}", partOrder.getId(), partOrder.getOrderCode());
        enqueueEvent("PartOrderCreated", partOrder, nvl(partOrder.getVersion(), 0L), false);
    }

    @Transactional
    public void recordPartOrderStatusChanged(PartOrder partOrder) {
        log.info("부품 주문 상태 변경 이벤트 발행 - 주문 ID: {}, 상태: {}", partOrder.getId(), partOrder.getStatus());
        enqueueEvent("PartOrderStatusChanged", partOrder, nvl(partOrder.getVersion(), 0L), false);
    }

    @Transactional
    public void recordPartOrderCompleted(PartOrder partOrder) {
        log.info("부품 주문 완료 이벤트 발행 - 주문 ID: {}, 주문 코드: {}", partOrder.getId(), partOrder.getOrderCode());
        enqueueEvent("PartOrderCompleted", partOrder, nvl(partOrder.getVersion(), 0L), false);
    }

    @Transactional
    public void recordPartOrderDeleted(PartOrder partOrder) {
        log.info("부품 주문 삭제 이벤트 발행 - 주문 ID: {}, 주문 코드: {}", partOrder.getId(), partOrder.getOrderCode());
        enqueueEvent("PartOrderDeleted", partOrder, nvl(partOrder.getVersion(), 0L), true);
    }

    // ===== 공통 헬퍼 =====
    private void enqueueEvent(String eventType, PartOrder partOrder, long version, Boolean deleted) {
        try {
            // 디버깅: externalPartOrderId 값 로깅
            log.info("디버깅 - PartOrder ID: {}, externalPartOrderId 값: {}",
                    partOrder.getId(), partOrder.getExternalPartOrderId());

            // Factory 정보 조회
            var factory = factoryProjectionRepository.findById(partOrder.getFactoryId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

            // PartOrderItem들을 PartOrderItemPayload로 변환
            List<PartOrderEvent.PartOrderItemPayload> itemPayloads = partOrder.getItems().stream()
                    .map(item -> {
                        var part = partProjectionRepository.findByPartId(item.getPartId())
                                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));
                        return new PartOrderEvent.PartOrderItemPayload(
                                part.getPartId(),
                                part.getName(),
                                part.getCode(),
                                item.getQuantity()
                        );
                    })
                    .collect(Collectors.toList());

            // 디버깅: 이벤트 생성 전 externalPartOrderId 값 다시 확인
            Long externalPartOrderId = partOrder.getExternalPartOrderId();
            log.info("디버깅 - 이벤트 생성 시 externalPartOrderId: {}", externalPartOrderId);

            PartOrderEvent evt = new PartOrderEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    version,
                    OffsetDateTime.now().toString(),
                    new PartOrderEvent.Payload(
                            partOrder.getId(),                    // partOrderId
                            partOrder.getOrderCode(),             // orderCode
                            partOrder.getFactoryId(),             // factoryId
                            factory.getBranchName(),              // factoryName
                            partOrder.getWarehouseId(),           // warehouseId
                            partOrder.getWarehouseName(),         // warehouseName
                            partOrder.getStatus().name(),         // status
                            partOrder.getRequiredDate() != null ? partOrder.getRequiredDate().toString() : null, // requiredDate
                            partOrder.getScheduledDate() != null ? partOrder.getScheduledDate().toString() : null, // scheduledDate
                            partOrder.getProgressRate(),          // progressRate
                            partOrder.getPriority() != null ? partOrder.getPriority().name() : null, // priority
                            partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null, // materialAvailability
                            externalPartOrderId,                  // externalPartOrderId - 올바른 위치
                            itemPayloads,                         // items
                            deleted                               // deleted
                    )
            );

            // 디버깅: 최종 이벤트 페이로드의 externalPartOrderId 확인
            log.info("디버깅 - 최종 이벤트 페이로드 externalPartOrderId: {}",
                    evt.payload().externalPartOrderId());

            JsonNode payload = objectMapper.valueToTree(evt);
            outboxRepository.save(
                    FactoryOutbox.ready(
                            partOrder.getId(),
                            eventType,
                            UUID.fromString(evt.eventId()),
                            payload
                    )
            );

            log.debug("부품 주문 이벤트 Outbox에 저장 완료 - 이벤트 ID: {}, 타입: {}", evt.eventId(), eventType);

        } catch (Exception e) {
            log.error("부품 주문 이벤트 직렬화 실패 - 주문 ID: {}, 이벤트 타입: {}, 오류: {}",
                partOrder.getId(), eventType, e.getMessage(), e);
            throw new IllegalStateException("Serialize " + eventType + " event failed", e);
        }
    }

    private long nvl(Long v, long def) {
        return v == null ? def : v;
    }
}
