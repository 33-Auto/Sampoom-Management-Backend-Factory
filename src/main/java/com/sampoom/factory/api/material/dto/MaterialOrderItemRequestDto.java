package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.MaterialOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class MaterialOrderItemRequestDto {
    private Long materialId;
    private Long quantity;

    public static MaterialOrderItemRequestDto from(MaterialOrderItem item) {
        return MaterialOrderItemRequestDto.builder()
                .materialId(item.getMaterial().getId())
                .quantity(item.getQuantity())
                .build();
    }
}
