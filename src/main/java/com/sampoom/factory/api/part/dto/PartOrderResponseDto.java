package com.sampoom.factory.api.part.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PartOrderResponseDto {
    private Long orderId;
    private String warehouseName;
    private LocalDateTime orderDate;
    private String status;
    private String factoryName;
    private Long factoryId;

    // 날짜 관련 필드들
    private LocalDateTime requiredDate; // 고객이 요청한 필요일
    private LocalDateTime scheduledDate; // 예정일
    private Double progressRate; // 진행률 (0.0 ~ 1.0)
    private String rejectionReason; // 반려 사유
    private Integer dDay; // D-day

    // 새로 추가된 필드들
    private String priority; // 우선순위 (HIGH, MEDIUM, LOW)
    private String materialAvailability; // 자재가용성 (SUFFICIENT, INSUFFICIENT)

    private List<PartOrderItemDto> items;

    @Getter
    @Builder
    public static class PartOrderItemDto {
        private Long partId;
        private String partName;
        private String partCode;
        private String partGroup;
        private String partCategory;
        private Long quantity;
    }
}