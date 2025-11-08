package com.sampoom.factory.api.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BomEventDto {
    private UUID eventId;
    private String eventType;
    private OffsetDateTime occurredAt;
    private Long version;
    private Payload payload;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long bomId;
        private Long partId;
        private String partCode;
        private String partName;
        private String status;
        private String complexity;
        private Boolean deleted;
        private Double totalCost;                    // 새로 추가된 필드
        private List<BomMaterialPayload> materials;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomMaterialPayload {
        private Long materialId;
        private String materialName;
        private String materialCode;
        private String unit;
        private Double quantity;                     // Integer에서 Double로 변경
    }
}