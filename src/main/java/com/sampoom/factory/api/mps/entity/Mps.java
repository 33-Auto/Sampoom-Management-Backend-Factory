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
@Table(name = "mps")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLDelete(sql = "UPDATE mps SET deleted = true, deleted_at = now() WHERE mps_id = ?")
@SQLRestriction("deleted = false")
public class Mps extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mpsId;

    @Column(nullable = false)
    private Long partId;              // 부품 ID

    @Column(nullable = false)
    private Long warehouseId;         // 창고 ID

    @Column(nullable = false)
    private Long factoryId;           // 공장 ID

    @Column(nullable = false)
    private Integer standardQuantity;  // 기준 수량

    @Column(nullable = false)
    private Integer expectedInventory; // 예상 재고

    @Column(nullable = false)
    private Integer forecastQuantity;  // 예측 수량

    @Column(nullable = false)
    private Integer totalProduction;   // 총 생산량

    @Column(nullable = false)
    private Integer safetyStock;       // 안전 재고

    @Column(nullable = false)
    private Integer leadTime;          // 리드 타임

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MpsStatus status;          // MPS 상태

    @Column(nullable = false)
    private LocalDate targetDate;      // 목표 완료 날짜 (예: 1/31)

    @Column(nullable = false)
    private LocalDate startDate;       // 계산된 시작 날짜 (예: 11/10)

    @Column(nullable = false)
    private Integer productionCycles;  // 생산 회수 (예: 9번)

    @Column(nullable = false)
    private Integer bufferDays;        // 여유 시간 (일)


    // MPS 계산 메서드
    public static Mps calculateMps(Long partId, Integer standardQuantity, Integer expectedInventory,
                                   Integer forecastQuantity, Integer safetyStock, Integer leadTime,
                                   LocalDate targetDate, Integer bufferDays) {

        // 필요 생산량 = 예측 수량 + 안전 재고 - 예상 재고
        int requiredQuantity = forecastQuantity + safetyStock - expectedInventory;

        // 생산 회수 계산: 필요 생산량이 0 이하면 생산 불필요
        int productionCycles = requiredQuantity > 0 ?
            (int) Math.ceil((double) requiredQuantity / standardQuantity) : 0;

        // 총 생산량 = 예측 수량 (실제 필요한 양), 생산이 필요 없으면 0
        int totalProduction = requiredQuantity > 0 ? forecastQuantity : 0;

        // 걸리는 날짜 = 생산 회수 * 리드 타임
        int requiredDays = productionCycles * leadTime;

        // 시작 날짜 = 목표 날짜 - 걸리는 날짜 - 여유 시간
        LocalDate startDate = targetDate.minusDays(requiredDays + bufferDays);

        return Mps.builder()
                .partId(partId)
                .standardQuantity(standardQuantity)
                .expectedInventory(expectedInventory)
                .forecastQuantity(forecastQuantity)
                .totalProduction(totalProduction)
                .safetyStock(safetyStock)
                .leadTime(leadTime)
                .status(MpsStatus.PLANNED)
                .targetDate(targetDate)
                .startDate(startDate)
                .productionCycles(productionCycles)
                .bufferDays(bufferDays)
                .build();
    }

    // 상태 업데이트 메서드
    public Mps updateStatus(MpsStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .build();
    }

    // MPS 재계산 메서드
    public Mps recalculate(Integer expectedInventory, Integer forecastQuantity, Integer safetyStock,
                          LocalDate targetDate, Integer bufferDays) {

        // 필요 생산량 = 예측 수량 + 안전 재고 - 예상 재고
        int requiredQuantity = forecastQuantity + safetyStock - expectedInventory;

        // 생산 회수 계산: 필요 생산량이 0 이하면 생산 불필요
        int productionCycles = requiredQuantity > 0 ?
            (int) Math.ceil((double) requiredQuantity / this.standardQuantity) : 0;

        // 총 생산량 = 예측 수량 (실제 필요한 양), 생산이 필요 없으면 0
        int totalProduction = requiredQuantity > 0 ? forecastQuantity : 0;

        // 걸리는 날짜 = 생산 회수 * 리드 타임
        int requiredDays = productionCycles * this.leadTime;

        // 시작 날짜 = 목표 날짜 - 걸리는 날짜 - 여유 시간
        LocalDate newStartDate = targetDate.minusDays(requiredDays + bufferDays);

        return this.toBuilder()
                .expectedInventory(expectedInventory)
                .forecastQuantity(forecastQuantity)
                .totalProduction(totalProduction)
                .safetyStock(safetyStock)
                .targetDate(targetDate)
                .startDate(newStartDate)
                .productionCycles(productionCycles)
                .bufferDays(bufferDays)
                .build();
    }
}
