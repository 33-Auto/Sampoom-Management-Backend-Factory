package com.sampoom.factory.api.part.dto;

import com.sampoom.factory.api.part.entity.MaterialAvailability;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.entity.PartOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MPS 주문 테스트용 간단한 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpsOrderInfoDto {

    private Long orderId;
    private String orderCode;
    private PartOrderType orderType;
    private PartOrderStatus status;
    private LocalDateTime minimumStartDate;
    private LocalDateTime scheduledDate;
    private MaterialAvailability materialAvailability;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MpsOrderInfoDto from(com.sampoom.factory.api.part.entity.PartOrder partOrder) {
        return MpsOrderInfoDto.builder()
                .orderId(partOrder.getId())
                .orderCode(partOrder.getOrderCode())
                .orderType(partOrder.getOrderType())
                .status(partOrder.getStatus())
                .minimumStartDate(partOrder.getMinimumStartDate())
                .scheduledDate(partOrder.getScheduledDate())
                .materialAvailability(partOrder.getMaterialAvailability())
                .createdAt(partOrder.getCreatedAt())
                .updatedAt(partOrder.getUpdatedAt())
                .build();
    }
}
