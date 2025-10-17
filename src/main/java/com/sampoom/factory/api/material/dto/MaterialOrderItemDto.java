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
public  class MaterialOrderItemDto {
    private Long materialId;
    private String materialName;
    private Long quantity;

    public static MaterialOrderItemDto from(MaterialOrderItem item) {
        return MaterialOrderItemDto.builder()
                .materialId(item.getMaterial().getId())
                .materialName(item.getMaterial().getName())
                .quantity(item.getQuantity())
                .build();
    }
}
