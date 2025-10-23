package com.sampoom.factory.api.factory.outbox;

public enum OutboxStatus {
    READY,
    PUBLISHED,
    FAILED,
    DEAD
}