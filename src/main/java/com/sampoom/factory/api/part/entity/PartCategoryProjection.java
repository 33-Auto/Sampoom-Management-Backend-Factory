package com.sampoom.factory.api.part.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "part_category_projection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("deleted = false")
public class PartCategoryProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                  // Projection 전용 ID

    @Column(nullable = false, unique = true)
    private Long categoryId;          // 원본 Category ID

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private String categoryCode;

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
    public PartCategoryProjection updateFromEvent(String categoryName, String categoryCode,
                                                Boolean deleted, UUID lastEventId,
                                                Long version, OffsetDateTime sourceUpdatedAt) {
        return this.toBuilder()
                .categoryName(categoryName)
                .categoryCode(categoryCode)
                .deleted(deleted)
                .lastEventId(lastEventId)
                .version(version)
                .sourceUpdatedAt(sourceUpdatedAt)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
