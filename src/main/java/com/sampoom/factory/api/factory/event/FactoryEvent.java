package com.sampoom.factory.api.factory.event;

public record FactoryEvent(
        String eventId,
        String eventType,        // "FactoryCreated" | "FactoryUpdated" | ...
        Long version,   // 1
        String occurredAt,       // ISO-8601
        Payload payload
){
    public record Payload(Long factoryId, String name, String address, String status,Boolean deleted) {}
}