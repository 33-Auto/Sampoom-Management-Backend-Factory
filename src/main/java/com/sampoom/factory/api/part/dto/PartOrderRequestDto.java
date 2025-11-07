package com.sampoom.factory.api.part.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartOrderRequestDto {
    private Long warehouseId; // 창고 ID 추가
    private String warehouseName;
    private LocalDateTime requiredDate; // 고객이 요청한 필요일
    private Long externalPartOrderId; // 외부 시스템의 주문 ID
    private List<PartOrderItemRequestDto> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartOrderItemRequestDto {
        private Long partId;
        private Long quantity;
        private Long materialId; // 이벤트에서 사용하는 material ID
        private Integer requestQuantity; // 이벤트에서 사용하는 요청 수량
    }
}