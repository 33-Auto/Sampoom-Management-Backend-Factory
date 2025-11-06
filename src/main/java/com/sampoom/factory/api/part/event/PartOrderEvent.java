package com.sampoom.factory.api.part.event;

import java.util.List;

public record PartOrderEvent(
        String eventId,
        String eventType,        // "PartOrderCreated" | "PartOrderStatusChanged" | ...
        Long version,
        String occurredAt,       // ISO-8601
        Payload payload
) {
    public record Payload(
            Long partOrderId,
            String orderCode,
            Long factoryId,
            String factoryName,
            Long warehouseId,
            String warehouseName,
            String status,
            String requiredDate,
            String scheduledDate,
            Double progressRate,
            String priority,
            String materialAvailability,
            List<PartOrderItemPayload> items,
            Boolean deleted
    ) {}

    public record PartOrderItemPayload(
            Long partId,
            String partName,
            String partCode,
            Long quantity
    ) {}
}
