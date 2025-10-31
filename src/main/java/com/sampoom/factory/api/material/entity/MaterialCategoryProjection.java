package com.sampoom.factory.api.material.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "material_category_projection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("deleted = false")
public class MaterialCategoryProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Projection 전용 ID

    @Column(nullable = false, unique = true)
    private Long categoryId; // 원본 Category ID

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

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

    // 업데이트 메서드
    public MaterialCategoryProjection updateFromEvent(String name, String code, Boolean deleted,
                                                      UUID lastEventId, Long version, OffsetDateTime sourceUpdatedAt) {
        return this.toBuilder()
                .name(name)
                .code(code)
                .deleted(deleted)
                .lastEventId(lastEventId)
                .version(version)
                .sourceUpdatedAt(sourceUpdatedAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}