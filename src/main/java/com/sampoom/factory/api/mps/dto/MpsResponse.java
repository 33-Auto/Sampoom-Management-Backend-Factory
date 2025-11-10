package com.sampoom.factory.api.mps.dto;

import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpsResponse {

    private Long mpsId;
    private Long partId;
    private Long warehouseId;
    private Long factoryId;
    private Integer standardQuantity;
    private Integer expectedInventory;
    private Integer forecastQuantity;
    private Integer totalProduction;
    private Integer safetyStock;
    private Integer leadTime;
    private MpsStatus status;
    private LocalDate targetDate;
    private LocalDate startDate;
    private Integer productionCycles;
    private Integer bufferDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MpsResponse from(Mps mps) {
        return MpsResponse.builder()
                .mpsId(mps.getMpsId())
                .partId(mps.getPartId())
                .warehouseId(mps.getWarehouseId())
                .factoryId(mps.getFactoryId())
                .standardQuantity(mps.getStandardQuantity())
                .expectedInventory(mps.getExpectedInventory())
                .forecastQuantity(mps.getForecastQuantity())
                .totalProduction(mps.getTotalProduction())
                .safetyStock(mps.getSafetyStock())
                .leadTime(mps.getLeadTime())
                .status(mps.getStatus())
                .targetDate(mps.getTargetDate())
                .startDate(mps.getStartDate())
                .productionCycles(mps.getProductionCycles())
                .bufferDays(mps.getBufferDays())
                .createdAt(mps.getCreatedAt())
                .updatedAt(mps.getUpdatedAt())
                .build();
    }
}
