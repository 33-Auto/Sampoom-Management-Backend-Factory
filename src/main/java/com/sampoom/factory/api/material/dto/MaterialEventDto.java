package com.sampoom.factory.api.material.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private MaterialEventDto.Payload payload;
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long materialId;
        private String materialCode;
        private String name;
        private String materialUnit;
        private Integer baseQuantity;
        private Integer standardQuantity;     // 새로 추가된 필드
        private Integer leadTime;
        private Boolean deleted;
        private Long materialCategoryId;
        private Long standardCost;
        private Long standardTotalCost;      // 새로 추가된 필드
    }
}
