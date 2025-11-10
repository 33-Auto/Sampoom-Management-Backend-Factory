package com.sampoom.factory.api.mps.entity;

import com.sampoom.factory.common.entitiy.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "mps_plan")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE mps_plan SET deleted = true, deleted_at = now() WHERE mps_plan_id = ?")
@SQLRestriction("deleted = false")
public class MpsPlan extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mpsPlanId;

    @Column(nullable = false)
    private Integer cycleNumber;       // 생산 회차 (1, 2, 3, ... productionCycles)

    @Column(nullable = false)
    private LocalDate requiredDate;    // 완료 요구일

    @Column(nullable = false)
    private Integer productionQuantity; // 해당 회차 생산량

    @Column(nullable = false)
    private Integer remainingTotalProduction; // 남은 총 생산량

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MpsPlanStatus status = MpsPlanStatus.PLANNED; // MPS Plan 상태

    // MPS와의 직접 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mps_id", nullable = false)
    private Mps mps;

    // 상태 업데이트 메서드
    public MpsPlan updateStatus(MpsPlanStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .build();
    }

    // 남은 총 생산량 업데이트 메서드
    public MpsPlan updateRemainingProduction(Integer newRemainingProduction) {
        return this.toBuilder()
                .remainingTotalProduction(newRemainingProduction)
                .build();
    }
}
