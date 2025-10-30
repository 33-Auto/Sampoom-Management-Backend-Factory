package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.api.part.entity.PartProjection;
import com.sampoom.factory.api.part.entity.PartStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "material_projection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("deleted = false")
public class MaterialProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                  // Projection 전용 ID

    @Column(nullable = false, unique = true)
    private Long materialId;              // 원본 Part ID

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String materialUnit;

    @Column(nullable = false)
    private Integer baseQuantity;

    @Column(nullable = false)
    private Integer leadTime;

    @Column(nullable = false)
    private Long categoryId;

    // --- 동기화 안전 메타(둘 중 하나 이상 필수) ---
    @Column(nullable = false)
    private Long version;

    @Column(name = "last_event_id", columnDefinition = "uuid")
    private UUID lastEventId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    // 시간은 타 서비스와 교환되므로 OffsetDateTime 권장
    @Column
    private OffsetDateTime sourceUpdatedAt;  // 원본 updatedAt

    @Column(nullable = false)
    private OffsetDateTime updatedAt;        // 프로젝션 갱신 시각

    // 업데이트 메서드
    public MaterialProjection updateFromEvent(String code, String name, String materialUnit,
                                          Integer baseQuantity, Integer leadTime,
                                           Boolean deleted,
                                           Long categoryId,
                                          UUID lastEventId,
                                          Long version, OffsetDateTime sourceUpdatedAt) {
        return this.toBuilder()
                .code(code)
                .name(name)
                .materialUnit(materialUnit)
                .baseQuantity(baseQuantity)
                .leadTime(leadTime)
                .deleted(deleted)
                .categoryId(categoryId)
                .lastEventId(lastEventId)
                .version(version)
                .sourceUpdatedAt(sourceUpdatedAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
