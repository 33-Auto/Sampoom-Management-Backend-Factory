package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.entity.*;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MPS 주문 자동 MRP 적용 스케줄러 테스트")
class PartOrderSchedulerServiceTest {

    @Mock
    private PartOrderRepository partOrderRepository;

    @Mock
    private PartOrderService partOrderService;

    @InjectMocks
    private PartOrderSchedulerService partOrderSchedulerService;

    private PartOrder mpsOrder;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();

        mpsOrder = PartOrder.builder()
                .id(1L)
                .orderCode("MPS-TEST-001")
                .factoryId(1L)
                .status(PartOrderStatus.PLAN_CONFIRMED)
                .orderType(PartOrderType.MPS)
                .minimumStartDate(testTime)
                .materialAvailability(MaterialAvailability.SUFFICIENT)
                .build();
    }

    @Test
    @DisplayName("MPS 주문이 시작일에 자동으로 MRP 결과 적용되어야 함")
    void shouldAutoApplyMrpForMpsOrdersAtStartDate() {
        // Given
        when(partOrderRepository.findByOrderTypeAndStatusAndMinimumStartDateBetween(
                eq(PartOrderType.MPS),
                eq(PartOrderStatus.PLAN_CONFIRMED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(mpsOrder));

        // When
        partOrderSchedulerService.autoApplyMrpForMpsOrders();

        // Then
        verify(partOrderRepository).findByOrderTypeAndStatusAndMinimumStartDateBetween(
                eq(PartOrderType.MPS),
                eq(PartOrderStatus.PLAN_CONFIRMED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(partOrderService).applyMrpResultsAutomatically(1L);
    }

    @Test
    @DisplayName("처리 대상 MPS 주문이 없으면 아무 작업도 하지 않아야 함")
    void shouldDoNothingWhenNoMpsOrdersToProcess() {
        // Given
        when(partOrderRepository.findByOrderTypeAndStatusAndMinimumStartDateBetween(
                any(PartOrderType.class),
                any(PartOrderStatus.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        // When
        partOrderSchedulerService.autoApplyMrpForMpsOrders();

        // Then
        verify(partOrderRepository).findByOrderTypeAndStatusAndMinimumStartDateBetween(
                any(PartOrderType.class),
                any(PartOrderStatus.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(partOrderService, never()).applyMrpResultsAutomatically(any());
    }

    @Test
    @DisplayName("MRP 적용 실패 시에도 다른 주문은 계속 처리되어야 함")
    void shouldContinueProcessingOtherOrdersWhenOneFails() {
        // Given
        PartOrder mpsOrder2 = PartOrder.builder()
                .id(2L)
                .orderCode("MPS-TEST-002")
                .factoryId(1L)
                .status(PartOrderStatus.PLAN_CONFIRMED)
                .orderType(PartOrderType.MPS)
                .minimumStartDate(testTime)
                .materialAvailability(MaterialAvailability.INSUFFICIENT)
                .build();

        when(partOrderRepository.findByOrderTypeAndStatusAndMinimumStartDateBetween(
                any(PartOrderType.class),
                any(PartOrderStatus.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(mpsOrder, mpsOrder2));

        // 첫 번째 주문 처리 실패
        doThrow(new RuntimeException("구매요청 실패")).when(partOrderService).applyMrpResultsAutomatically(1L);
        // 두 번째 주문 처리 성공
        doNothing().when(partOrderService).applyMrpResultsAutomatically(2L);

        // When
        partOrderSchedulerService.autoApplyMrpForMpsOrders();

        // Then
        verify(partOrderService).applyMrpResultsAutomatically(1L);
        verify(partOrderService).applyMrpResultsAutomatically(2L);
    }
}
