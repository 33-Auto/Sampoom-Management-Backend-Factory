package com.sampoom.factory.api.factory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BranchFactoryDistanceEventDto {
    private UUID eventId;
    private String eventType;
    private Long version;
    private OffsetDateTime occurredAt;
    private Payload payload;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private Long distanceId;
        private Long branchId;
        private Long factoryId;
        private Double distanceKm;
        private String branchName;
        private String factoryName;
    }
}
