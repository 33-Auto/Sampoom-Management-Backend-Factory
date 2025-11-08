package com.sampoom.factory.api.purchase.service;

import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.purchase.dto.PurchaseEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseEventService {

    private final FactoryMaterialRepository factoryMaterialRepository;
    private final MaterialProjectionRepository materialProjectionRepository;

    @Transactional
    public void handlePurchaseEvent(PurchaseEventDto event) {
        PurchaseEventDto.Payload payload = event.getPayload();

        log.info("구매 이벤트 처리 시작 - 이벤트타입: {}, 주문ID: {}, 공장ID: {}, 공장명: {}",
                event.getEventType(), payload.getOrderId(), payload.getFactoryId(), payload.getFactoryName());

        // PurchaseOrderReceived 이벤트만 처리
        if (!"PurchaseOrderReceived".equals(event.getEventType())) {
            log.debug("PurchaseOrderReceived가 아닌 이벤트 스킵 - 이벤트타입: {}", event.getEventType());
            return;
        }

        // RECEIVED 상태인 경우에만 입고 처리
        if (!"RECEIVED".equals(payload.getStatus())) {
            log.info("RECEIVED 상태가 아니므로 입고 처리 스킵 - 현재 상태: {}", payload.getStatus());
            return;
        }

        // 삭제된 주문인 경우 처리하지 않음
        if (Boolean.TRUE.equals(payload.getDeleted())) {
            log.info("삭제된 주문이므로 입고 처리 스킵 - 주문ID: {}", payload.getOrderId());
            return;
        }

        // materials가 null이거나 비어있는 경우 처리하지 않음
        if (payload.getMaterials() == null || payload.getMaterials().isEmpty()) {
            log.info("자재 목록이 없으므로 입고 처리 스킵 - 주문ID: {}", payload.getOrderId());
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // 각 자재별로 입고 처리
        for (PurchaseEventDto.MaterialItem material : payload.getMaterials()) {
            try {
                processMaterialReceiving(payload.getFactoryId(), material);
                successCount++;
                log.info("자재 입고 처리 성공 - 공장ID: {}, 자재코드: {}, 자재명: {}, 수량: {}",
                        payload.getFactoryId(), material.getMaterialCode(),
                        material.getMaterialName(), material.getQuantity());
            } catch (Exception e) {
                failCount++;
                log.error("자재 입고 처리 실패 - 공장ID: {}, 자재코드: {}, 자재명: {}, 오류: {}",
                        payload.getFactoryId(), material.getMaterialCode(),
                        material.getMaterialName(), e.getMessage(), e);
            }
        }

        log.info("구매 주문 입고 처리 완료 - 주문ID: {}, 성공: {}, 실패: {}",
                payload.getOrderId(), successCount, failCount);

        if (failCount > 0) {
                        throw new RuntimeException(String.format(
                                        "구매 주문 입고 처리 중 일부 자재가 실패했습니다. 성공: %d, 실패: %d, 주문ID: %d",
                                        successCount, failCount, payload.getOrderId()));
                    }
    }

    private void processMaterialReceiving(Long factoryId, PurchaseEventDto.MaterialItem material) {
        // materialCode로 materialId 찾기
        var materialProjection = materialProjectionRepository.findByCode(material.getMaterialCode());
        if (materialProjection.isEmpty()) {
            throw new RuntimeException("자재를 찾을 수 없습니다 - 자재코드: " + material.getMaterialCode());
        }

        Long materialId = materialProjection.get().getMaterialId();

        // 해당 공장의 자재 재고 조회
        FactoryMaterial factoryMaterial = factoryMaterialRepository
                .findFirstByFactoryIdAndMaterialId(factoryId, materialId)
                .orElse(null);

        if (factoryMaterial == null) {
            // 기존 재고가 없으면 새로 생성
            factoryMaterial = FactoryMaterial.builder()
                    .factoryId(factoryId)
                    .materialId(materialId)
                    .quantity(material.getQuantity().doubleValue())  // Long을 Double로 변환
                    .build();

            factoryMaterialRepository.save(factoryMaterial);
            log.info("신규 자재 재고 생성 - 공장ID: {}, 자재ID: {}, 자재코드: {}, 초기수량: {}",
                    factoryId, materialId, material.getMaterialCode(), material.getQuantity());
        } else {
            // 기존 재고에 수량 추가
            Double previousQuantity = factoryMaterial.getQuantity();
            factoryMaterial.increaseQuantity(material.getQuantity().doubleValue());  // Long을 Double로 변환
            log.info("기존 자재 재고 증가 - 공장ID: {}, 자재ID: {}, 자재코드: {}, 이전수량: {}, 입고수량: {}, 현재수량: {}",
                    factoryId, materialId, material.getMaterialCode(),
                    previousQuantity, material.getQuantity(), factoryMaterial.getQuantity());
        }
    }
}
