package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.Material;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponseDto {
    private Long id;
    private String name;
    private String materialCode;
    private Long materialCategoryId;
    private String materialCategoryName;
    private Long quantity;

    public static MaterialResponseDto from(Material material) {
        return MaterialResponseDto.builder()
                .id(material.getId())
                .name(material.getName())
                .materialCode(material.getCode())
                .materialCategoryId(material.getMaterialCategory().getId())
                .materialCategoryName(material.getMaterialCategory().getName())
                .build();
    }

    public MaterialResponseDto withQuantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }
}