package com.sampoom.factory.api.factory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "branch_factory_distance")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BranchFactoryDistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long distanceId;          // 원본 Distance ID

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long factoryId;

    @Column(nullable = false)
    private Double distanceKm;

    @Column(nullable = false)
    private String branchName;

    @Column(nullable = false)
    private String factoryName;

    // --- 동기화 안전 메타 ---
    @Column(nullable = false)
    private Long version;

    @Column(name = "last_event_id", columnDefinition = "uuid")
    private UUID lastEventId;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime sourceUpdatedAt;

    // 업데이트 메서드
    public BranchFactoryDistance updateFromEvent(Long branchId, Long factoryId, Double distanceKm,
                                               String branchName, String factoryName,
                                               UUID lastEventId, Long version, OffsetDateTime sourceUpdatedAt) {
        return this.toBuilder()
                .branchId(branchId)
                .factoryId(factoryId)
                .distanceKm(distanceKm)
                .branchName(branchName)
                .factoryName(factoryName)
                .lastEventId(lastEventId)
                .version(version)
                .sourceUpdatedAt(sourceUpdatedAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
