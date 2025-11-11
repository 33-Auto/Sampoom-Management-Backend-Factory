package com.sampoom.factory.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class SoftDeleteEntity extends BaseTimeEntity {
    @Column(nullable = false)
    protected boolean deleted = false;

    protected LocalDateTime deletedAt;

    public void softDelete() {
        if (this.deleted) return;
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}