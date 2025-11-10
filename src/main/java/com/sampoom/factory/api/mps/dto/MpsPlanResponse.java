package com.sampoom.factory.api.mps.dto;

import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.entity.MpsPlanStatus;
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
public class MpsPlanResponse {

    private Long mpsPlanId;
    private Long mpsId;
    private Integer cycleNumber;
    private LocalDate requiredDate;
    private Integer productionQuantity;
    private Integer remainingTotalProduction;
    private MpsPlanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MpsPlanResponse from(MpsPlan mpsPlan) {
        return MpsPlanResponse.builder()
                .mpsPlanId(mpsPlan.getMpsPlanId())
                .mpsId(mpsPlan.getMps().getMpsId())
                .cycleNumber(mpsPlan.getCycleNumber())
                .requiredDate(mpsPlan.getRequiredDate())
                .productionQuantity(mpsPlan.getProductionQuantity())
                .remainingTotalProduction(mpsPlan.getRemainingTotalProduction())
                .status(mpsPlan.getStatus())
                .createdAt(mpsPlan.getCreatedAt())
                .updatedAt(mpsPlan.getUpdatedAt())
                .build();
    }
}
