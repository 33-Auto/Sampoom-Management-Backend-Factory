package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.bom.entity.BomMaterialProjection;
import com.sampoom.factory.api.bom.entity.BomProjection;
import com.sampoom.factory.api.bom.repository.BomMaterialProjectionRepository;
import com.sampoom.factory.api.bom.repository.BomProjectionRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.part.entity.MaterialAvailability;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderItem;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartOrderSchedulerService {

    private final PartOrderRepository partOrderRepository;
    private final BomProjectionRepository bomProjectionRepository;
    private final BomMaterialProjectionRepository bomMaterialProjectionRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final PartOrderEventService partOrderEventService; // 이벤트 서비스 추가

    /**
     * 매시간 예정일이 지난 진행중인 주문들을 자동으로 완료 처리
     */
    @Scheduled(cron = "0 0 * * * *") // 매시간 정각에 실행
    @Transactional
    public void autoCompleteOverdueOrders() {
        log.info("예정일 기준 주문 자동 완료 처리 시작");

        LocalDateTime now = LocalDateTime.now();

        // 진행 중이면서 예정일이 지난 주문들 조회
        List<PartOrder> overdueOrders = partOrderRepository
            .findByStatusAndScheduledDateBefore(PartOrderStatus.IN_PROGRESS, now);

        int completedCount = 0;
        int materialInsufficientCount = 0;

        for (PartOrder order : overdueOrders) {
            try {
                // 자재 가용성에 따른 완료 처리
                if (order.getMaterialAvailability() == MaterialAvailability.INSUFFICIENT) {
                    // 자재 부족 주문: 완료 시점에 자재 차감
                    deductMaterials(order);
                    order.completeWithMaterialDeduction();
                    materialInsufficientCount++;
                    log.info("자재 부족 주문 자동 완료 및 자재 차감: 주문ID={}, 주문코드={}",
                        order.getId(), order.getOrderCode());
                } else {
                    // 자재 충분 주문: 단순 완료 (이미 자재 차감됨)
                    boolean wasCompleted = order.autoCompleteIfOverdue();
                    if (wasCompleted) {
                        log.info("자재 충분 주문 자동 완료: 주문ID={}, 주문코드={}",
                            order.getId(), order.getOrderCode());
                    }
                }

                partOrderRepository.save(order);

                // 자동 완료된 주문에 대해 이벤트 발행
                partOrderEventService.recordPartOrderCompleted(order);

                completedCount++;

            } catch (Exception e) {
                log.error("주문 자동 완료 처리 실패: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage());
            }
        }

        log.info("예정일 기준 주문 자동 완료 처리 완료 - 완료된 주문: {}건 (자재부족: {}건)",
            completedCount, materialInsufficientCount);
    }

    // 자재 차감 로직 (PartOrderService와 동일)
    private void deductMaterials(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환
                factoryMaterial.decreaseQuantity(required);
            }
        }
    }

    /**
     * 매일 오전 9시에 진행률 업데이트 및 상태 점검
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시에 실행
    @Transactional
    public void updateProgressAndCheckStatus() {
        log.info("주문 진행률 업데이트 및 상태 점검 시작");

        // 진행 중인 모든 주문들의 진행률 업데이트
        List<PartOrder> inProgressOrders = partOrderRepository
            .findByStatus(PartOrderStatus.IN_PROGRESS);

        int updatedCount = 0;
        int autoCompletedCount = 0;

        for (PartOrder order : inProgressOrders) {
            try {
                // 이전 상태 저장
                PartOrderStatus previousStatus = order.getStatus();

                // 진행률 계산 (이미 자동 완료 로직 포함)
                order.calculateProgressByDate();
                partOrderRepository.save(order);
                updatedCount++;

                // 자동 완료되었는지 확인 및 이벤트 발행
                if (previousStatus == PartOrderStatus.IN_PROGRESS && order.getStatus() == PartOrderStatus.COMPLETED) {
                    autoCompletedCount++;

                    // 진행률 업데이트 중 자동 완료된 주문에 대해 이벤트 발행
                    partOrderEventService.recordPartOrderCompleted(order);

                    log.info("진행률 업데이트 중 자동 완료된 주문: 주문ID={}, 주문코드={}",
                        order.getId(), order.getOrderCode());
                }

            } catch (Exception e) {
                log.error("주문 진행률 업데이트 실패: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage());
            }
        }

        log.info("주문 진행률 업데이트 완료 - 업데이트: {}건, 자동완료: {}건",
            updatedCount, autoCompletedCount);
    }

    /**
     * 매주 월요일 오전 8시에 지연된 주문들 점검
     */
    @Scheduled(cron = "0 0 8 * * MON") // 매주 월요일 오전 8시
    @Transactional(readOnly = true)
    public void checkDelayedOrders() {
        log.info("지연된 주문 점검 시작");

        List<PartOrder> delayedOrders = partOrderRepository
            .findByStatus(PartOrderStatus.DELAYED);

        for (PartOrder order : delayedOrders) {
            log.warn("지연된 주문 발견: 주문ID={}, 주문코드={}, 예정일={}, D-Day={}",
                order.getId(), order.getOrderCode(), order.getScheduledDate(), order.getDDay());
        }

        log.info("지연된 주문 점검 완료 - 총 {}건", delayedOrders.size());
    }
}
