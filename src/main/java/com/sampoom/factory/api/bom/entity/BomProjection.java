package com.sampoom.factory.api.bom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bom_projection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("deleted = false")
public class BomProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bomId;

    @Column(nullable = false)
    private Long partId;

    @Column(nullable = false)
    private String partCode;

    @Column(nullable = false)
    private String partName;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String complexity;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false)
    private Long version;

    @Column(name = "last_event_id", columnDefinition = "uuid")
    private UUID lastEventId;

    @Column
    private OffsetDateTime sourceUpdatedAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;


    public BomProjection updateFromEvent(String partCode, String partName, String status, String complexity, Boolean deleted,
                                         UUID lastEventId, Long version, OffsetDateTime sourceUpdatedAt) {
        return this.toBuilder()
                .partCode(partCode)
                .partName(partName)
                .status(status)
                .complexity(complexity)
                .deleted(deleted)
                .lastEventId(lastEventId)
                .version(version)
                .sourceUpdatedAt(sourceUpdatedAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}