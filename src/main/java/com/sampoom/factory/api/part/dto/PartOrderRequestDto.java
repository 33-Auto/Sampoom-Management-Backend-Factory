package com.sampoom.factory.api.part.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartOrderRequestDto {
    private String warehouseName;
    private List<PartOrderItemRequestDto> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartOrderItemRequestDto {
        private Long partId;
        private Long quantity;
    }
}