package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.api.material.entity.OrderStatus;
import com.sampoom.factory.api.material.entity.MaterialOrder;
import com.sampoom.factory.api.material.entity.MaterialOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialOrderResponseDto {
    private Long id;
    private String code;
    private Long factoryId;
    private String factoryName;
    private OrderStatus status;
    private LocalDateTime orderAt;
    private LocalDateTime receivedAt;
    private List<MaterialOrderItemDto> items;


    public static MaterialOrderResponseDto from(MaterialOrder order, List<MaterialOrderItem> orderItems, java.util.function.Function<Long, MaterialProjection> projectionResolver) {
        return MaterialOrderResponseDto.builder()
                .id(order.getId())
                .code(order.getCode())
                .factoryId(order.getFactoryId())
                .factoryName("") // factoryName은 별도로 설정 필요
                .status(order.getStatus())
                .orderAt(order.getOrderAt())
                .receivedAt(order.getReceivedAt())
                .items(orderItems.stream()
                        .map(item -> MaterialOrderItemDto.from(item, projectionResolver.apply(item.getMaterialId())))
                        .collect(Collectors.toList()))
                .build();
    }
}
