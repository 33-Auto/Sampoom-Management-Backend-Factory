package com.sampoom.factory.api.purchase.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseEventDto {
    private String eventId;
    private String eventType;
    private Long version;
    private String occurredAt;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long orderId;
        private String orderCode;
        private Long factoryId;
        private String factoryName;
        private String status;
        private LocalDateTime receivedAt;
        private Boolean deleted;
        private List<MaterialItem> materials;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialItem {
        private String unit;
        private Long quantity;
        private String materialCode;
        private String materialName;
    }
}
