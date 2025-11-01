package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.MaterialOrderItem;
import com.sampoom.factory.api.material.entity.MaterialProjection;
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
    private String unit;
    private Long quantity;



    public static MaterialOrderItemDto from(MaterialOrderItem item, MaterialProjection materialProjection) {
        return MaterialOrderItemDto.builder()
                .materialId(materialProjection.getMaterialId())
                .materialName(materialProjection.getName())
                .unit(materialProjection.getMaterialUnit())
                .quantity(item.getQuantity())
                .build();
    }
}
