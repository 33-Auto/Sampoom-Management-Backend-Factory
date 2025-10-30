package com.sampoom.factory.api.part.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PartCategoryEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private PartCategoryPayload payload;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartCategoryPayload {
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
    }
}
