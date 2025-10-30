package com.sampoom.factory.api.part.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PartEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private PartPayload payload;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartPayload {
        private Long partId;
        private String code;
        private String name;
        private String partUnit;
        private Integer baseQuantity;
        private Integer leadTime;
        private String status;
        private Boolean deleted;
        private Long groupId;
        private Long categoryId;
    }
}
