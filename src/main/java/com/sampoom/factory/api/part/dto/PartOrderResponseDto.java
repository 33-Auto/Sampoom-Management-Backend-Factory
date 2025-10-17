package com.sampoom.factory.api.part.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PartOrderResponseDto {
    private Long orderId;
    private String warehouseName;
    private LocalDateTime orderDate;
    private String status;
    private List<PartOrderItemDto> items;

    @Getter
    @Builder
    public static class PartOrderItemDto {
        private Long partId;
        private String partName;
        private String partCode;
        private String partGroup;
        private String partCategory;
        private Long quantity;
    }
}