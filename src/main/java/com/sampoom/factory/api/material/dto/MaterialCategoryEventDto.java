package com.sampoom.factory.api.material.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCategoryEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private MaterialCategoryEventDto.Payload payload;
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long categoryId;
        private String name;
        private String code;
        private Boolean deleted;
    }
}
