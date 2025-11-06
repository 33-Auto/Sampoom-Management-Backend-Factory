package com.sampoom.factory.api.part.entity;

import com.sampoom.factory.common.entitiy.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "part_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_order_id")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version; // 낙관적 락을 위한 버전 필드

    @Column(name = "factory_id", nullable = false)
    private Long factoryId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    private PartOrderStatus status;

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "required_date", nullable = false)
    private LocalDateTime requiredDate; // 고객이 요청한 필요일

    @Column(name = "order_code", unique = true)
    private String orderCode; // WO-2025-001 형태의 주문 코드

    // 새로 추가된 필드들
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate; // MRP 실행 후 계산된 예정일

    @Column(name = "progress_rate")
    @Builder.Default
    private Double progressRate = 0.0; // 진행률 (0.0 ~ 1.0)

    @Column(name = "rejection_reason")
    private String rejectionReason; // 반려 사유

    @Column(name = "d_day")
    private Integer dDay; // 현재 날짜 기준 예정일까지 D-day

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private PartOrderPriority priority; // 우선순위 (높음, 보통, 낮음)

    @Enumerated(EnumType.STRING)
    @Column(name = "material_availability")
    private MaterialAvailability materialAvailability; // 자재가용성 (충분, 부족)

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private PartOrderStatus previousStatus; // 이전 상태 (생산계획에서 보여줄 상태)

    @OneToMany(mappedBy = "partOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PartOrderItem> items = new ArrayList<>();

    public void updateStatus(PartOrderStatus status) {
        this.status = status;
    }

    public void updateScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
        updateDDay();
    }

    public void updateProgressRate(Double progressRate) {
        this.progressRate = progressRate;
    }

    public void reject(String rejectionReason) {
        this.status = PartOrderStatus.REJECTED;
        this.rejectionReason = rejectionReason;
    }

    public void confirmPlan() {
        this.status = PartOrderStatus.PLAN_CONFIRMED;
    }

    public void markAsDelayed() {
        this.status = PartOrderStatus.DELAYED;
    }

    public void startProgress() {
        // 이전 상태를 저장한 후 IN_PROGRESS로 변경
        this.previousStatus = this.status;
        this.status = PartOrderStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = PartOrderStatus.COMPLETED;
        this.progressRate = 1.0;
    }

    // 자재 차감과 함께 생산 완료
    public void completeWithMaterialDeduction() {
        this.status = PartOrderStatus.COMPLETED;
        this.progressRate = 1.0;
    }

    public void requestPurchase() {
        this.status = PartOrderStatus.PURCHASE_REQUEST;
    }

    // 우선순위 계산 및 설정 (필요일까지의 남은 일수 기준)
    public void calculateAndSetPriority() {
        if (requiredDate != null) {
            LocalDateTime now = LocalDateTime.now();
            long daysUntilRequired = java.time.temporal.ChronoUnit.DAYS.between(
                now.toLocalDate(),
                requiredDate.toLocalDate()
            );

            if (daysUntilRequired <= 3) {
                this.priority = PartOrderPriority.HIGH; // 3일 이하: 높음
            } else if (daysUntilRequired <= 7) {
                this.priority = PartOrderPriority.MEDIUM; // 4-7일: 보통
            } else {
                this.priority = PartOrderPriority.LOW; // 8일 이상: 낮음
            }
        } else {
            this.priority = PartOrderPriority.MEDIUM; // 기본값
        }
    }

    // 자재가용성 설정
    public void updateMaterialAvailability(MaterialAvailability materialAvailability) {
        this.materialAvailability = materialAvailability;
    }

    private void updateDDay() {
        if (scheduledDate != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                scheduledDate.toLocalDate()
            );
            this.dDay = (int) daysBetween;
        }
    }

    // 진행률 계산을 위한 메서드 (현재 날짜와 예정일 기준)
    public void calculateProgressByDate() {
        if (scheduledDate != null && orderDate != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(scheduledDate)) {
                // 예정일이 지났으면 진행률 100%
                this.progressRate = 1.0;
                // 진행중 상태인 경우 자동으로 완료 처리
                if (this.status == PartOrderStatus.IN_PROGRESS) {
                    this.status = PartOrderStatus.COMPLETED;
                }
            } else {
                // 주문일부터 예정일까지의 전체 기간 대비 현재까지의 진행률
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(orderDate.toLocalDate(), scheduledDate.toLocalDate());
                long passedDays = java.time.temporal.ChronoUnit.DAYS.between(orderDate.toLocalDate(), now.toLocalDate());

                if (totalDays > 0) {
                    this.progressRate = Math.min(1.0, (double) passedDays / totalDays);
                }
            }
        }
        updateDDay();
    }

    // 예정일 기준 자동 완료 처리 (스케줄러에서 호출용)
    public boolean autoCompleteIfOverdue() {
        if (scheduledDate != null && this.status == PartOrderStatus.IN_PROGRESS) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(scheduledDate)) {
                this.status = PartOrderStatus.COMPLETED;
                this.progressRate = 1.0;
                updateDDay();
                return true; // 완료 처리됨
            }
        }
        return false; // 완료 처리되지 않음
    }

    // 예정일 경과 여부 확인
    public boolean isOverdue() {
        return scheduledDate != null && LocalDateTime.now().isAfter(scheduledDate);
    }
}