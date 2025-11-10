package com.sampoom.factory.api.mps.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartForecastEvent {

    private String eventId;
    private PartForecastPayload payload;
    private Integer version;
    private String eventType;
    private OffsetDateTime occurredAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartForecastPayload {
        private Integer stock;           // 현재 재고 (예상 재고로 사용)
        private Long partId;            // 부품 ID

        private LocalDateTime demandMonth;  // 수요 월 (타임존 없는 형식으로 변경)

        private Long warehouseId;       // 창고 ID
        private Integer demandQuantity; // 수요 수량 (예측 수량으로 사용)
    }
}
