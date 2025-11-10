package com.sampoom.factory.api.part.service;


import com.sampoom.factory.api.part.dto.OrderToFactoryEventDto;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderToFactoryEventService {
    private final PartOrderService partOrderService;

    @Transactional
    public void processOrderToFactoryEvent(OrderToFactoryEventDto event) {
        log.info("Processing order-to-factory event for partOrderId: {}, warehouseId: {}",
                event.getPartOrderId(), event.getWarehouseId());

        // 이벤트 데이터를 PartOrderRequestDto로 변환
        PartOrderRequestDto request = convertEventToRequest(event);

        // 부품 주문 생성
        List<PartOrderResponseDto> responses = partOrderService.createPartOrdersSeparately(request);

        log.info("Created {} part orders from event. PartOrderId: {}, WarehouseId: {}",
                responses.size(), event.getPartOrderId(), event.getWarehouseId());
    }

    private PartOrderRequestDto convertEventToRequest(OrderToFactoryEventDto event) {

        if (event.getItems() == null || event.getItems().isEmpty()) {
                        throw new IllegalArgumentException("OrderToFactoryEvent items must not be null or empty");
                    }
        // OrderToFactoryEventDto를 PartOrderRequestDto로 변환
        List<PartOrderRequestDto.PartOrderItemRequestDto> items = event.getItems().stream()
                .map(item -> PartOrderRequestDto.PartOrderItemRequestDto.builder()
                        .partId(item.getId()) // materialId → partId로 변경
                        .quantity(item.getDelta().longValue()) // requestQuantity → quantity로 변경 (Integer → Long 변환)
                        .build())
                .collect(Collectors.toList());

        // 이벤트의 requiredDate 사용, 없으면 기본값 (현재 시간 + 7일)
        LocalDateTime requiredDate = event.getRequiredDate() != null ?
                event.getRequiredDate() : LocalDateTime.now().plusDays(7);

        return PartOrderRequestDto.builder()
                .items(items)
                .warehouseId(event.getWarehouseId())
                .warehouseName(event.getWarehouseName())
                .requiredDate(requiredDate) // 이벤트의 requiredDate 사용
                .externalPartOrderId(event.getPartOrderId()) // 외부 주문 ID 저장용
                .build();
    }

}
