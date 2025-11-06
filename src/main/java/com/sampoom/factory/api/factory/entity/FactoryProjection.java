package com.sampoom.factory.api.factory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "factory_projection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FactoryProjection {

    @Id
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_code", nullable = false)
    private String branchCode;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FactoryStatus status;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    // --- 동기화 안전 메타 ---
    @Column(nullable = false)
    private Long version;

    @Column(name = "last_event_id", columnDefinition = "uuid")
    private UUID lastEventId;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime sourceUpdatedAt;

    public void updateFromEvent(String branchCode, String branchName, String address,
                               Double latitude, Double longitude, FactoryStatus status, Boolean deleted,
                               Long version, UUID eventId) {
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.deleted = deleted;
        this.version = version;
        this.lastEventId = eventId;
    }
}
