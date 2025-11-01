package com.sampoom.factory.api.purchase.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseRequestDto {
    private Long factoryId;
    private String factoryName;
    private LocalDate requiredAt;
    private String requesterName;
    private List<PurchaseItemDto> items;

    @Data
    @Builder
    public static class PurchaseItemDto {
        private String materialCode;
        private String materialName;
        private String unit;
        private Long quantity;
        private Long unitPrice;
    }
}
