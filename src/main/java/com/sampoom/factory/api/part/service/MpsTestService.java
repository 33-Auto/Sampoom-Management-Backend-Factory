package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.dto.MpsOrderInfoDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.entity.PartOrderType;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MPS 주문 테스트를 위한 유틸리티 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpsTestService {

    private final PartOrderRepository partOrderRepository;
    private final PartOrderService partOrderService;

    /**
     * 테스트용: MPS 주문의 최소 시작일을 현재 시간으로 강제 설정
     * 스케줄러 테스트를 위해 사용
     */
    @Transactional
    public void setMpsOrderStartDateToNow(Long orderId) {
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if (partOrder.getOrderType() != PartOrderType.MPS) {
            throw new RuntimeException("MPS 주문이 아닙니다: " + orderId);
        }

        LocalDateTime now = LocalDateTime.now();
        partOrder.updateMinimumStartDate(now);
        partOrderRepository.save(partOrder);

        log.info("테스트용 - MPS 주문 {}의 시작일을 현재 시간으로 설정: {}", orderId, now);
    }

    /**
     * 테스트용: MPS 주문을 계획확정 상태로 강제 변경
     */
    @Transactional
    public void setMpsOrderToPlanConfirmed(Long orderId) {
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        if (partOrder.getOrderType() != PartOrderType.MPS) {
            throw new RuntimeException("MPS 주문이 아닙니다: " + orderId);
        }

        // 강제로 상태 변경 (실제로는 MRP 실행 후 자동 설정됨)
        partOrder.confirmPlan();
        partOrderRepository.save(partOrder);

        log.info("테스트용 - MPS 주문 {}의 상태를 계획확정으로 변경", orderId);
    }

    /**
     * 테스트용: MPS 주문 생성 후 바로 MRP 실행하여 계획확정 상태까지 진행
     */
    @Transactional
    public Long createAndPrepareMpsOrderForTest(Long factoryId, Long partId, Long quantity) {
        // MPS 주문 생성
        LocalDateTime requiredDate = LocalDateTime.now().plusDays(7); // 일주일 후
        PartOrderResponseDto mpsOrder = partOrderService.createMpsPartOrder(
                factoryId,
                1L, // 임시 창고ID
                partId,
                quantity,
                requiredDate,
                999L, // 임시 MPS Plan ID
                "TEST-MPS-001"
        );

        // MRP 실행
        partOrderService.executeMRPLogic(
                partOrderRepository.findById(mpsOrder.getOrderId())
                        .orElseThrow(() -> new RuntimeException("생성된 주문을 찾을 수 없습니다"))
        );

        // 시작일을 현재 시간으로 설정 (테스트를 위해)
        setMpsOrderStartDateToNow(mpsOrder.getOrderId());

        log.info("테스트용 MPS 주문이 생성되고 준비되었습니다 - 주문 ID: {}", mpsOrder.getOrderId());
        return mpsOrder.getOrderId();
    }

    /**
     * 테스트용: 현재 자동 처리 대상인 MPS 주문들 조회
     */
    @Transactional(readOnly = true)
    public List<MpsOrderInfoDto> findMpsOrdersReadyForAutoProcessing() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfHour = startOfHour.plusHours(1).minusNanos(1);
        List<PartOrderStatus> targetStatuses = List.of(PartOrderStatus.PLAN_CONFIRMED, PartOrderStatus.DELAYED);

        List<PartOrder> partOrders = partOrderRepository.findByOrderTypeAndStatusInAndMinimumStartDateBetween(
                PartOrderType.MPS,
                targetStatuses,
                startOfHour,
                endOfHour
        );

        return partOrders.stream()
                .map(MpsOrderInfoDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 테스트용: MPS 주문 정보 출력
     */
    public void printMpsOrderInfo(Long orderId) {
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        log.info("=== MPS 주문 정보 ===");
        log.info("주문 ID: {}", partOrder.getId());
        log.info("주문 코드: {}", partOrder.getOrderCode());
        log.info("주문 타입: {}", partOrder.getOrderType());
        log.info("현재 상태: {}", partOrder.getStatus());
        log.info("최소 시작일: {}", partOrder.getMinimumStartDate());
        log.info("자재 가용성: {}", partOrder.getMaterialAvailability());
        log.info("현재 시간: {}", LocalDateTime.now());
        log.info("==================");
    }
}
