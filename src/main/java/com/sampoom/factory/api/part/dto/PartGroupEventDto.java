package com.sampoom.factory.api.part.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PartGroupEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private PartGroupPayload payload;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartGroupPayload {
        private Long groupId;
        private String groupName;
        private String groupCode;
        private Long categoryId;
    }
}
