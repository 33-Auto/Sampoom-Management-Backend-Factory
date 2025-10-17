package com.sampoom.factory.api.part.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PartOrderRequestDto {
    private String warehouseName;
    private List<PartOrderItemRequestDto> items;

    @Getter
    public static class PartOrderItemRequestDto {
        private Long partId;
        private Long quantity;
    }
}