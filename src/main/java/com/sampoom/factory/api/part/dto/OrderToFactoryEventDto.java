package com.sampoom.factory.api.part.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderToFactoryEventDto {
    @JsonProperty("items")
    private List<OrderItem> items;

    @JsonProperty("partOrderId")
    private Long partOrderId;

    @JsonProperty("warehouseId")
    private Long warehouseId;

    @JsonProperty("warehouseName")
    private String warehouseName;

    @JsonProperty("requiredDate")
    private LocalDateTime requiredDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("delta")
        private Integer delta;
    }
}
