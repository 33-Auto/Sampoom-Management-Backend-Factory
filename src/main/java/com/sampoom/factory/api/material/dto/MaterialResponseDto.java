package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.MaterialProjection;
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
    private String unit;
    private String materialCode;
    private Long materialCategoryId;
    private String materialCategoryName;
    private Long quantity;

    public static MaterialResponseDto from(MaterialProjection material) {
        return MaterialResponseDto.builder()
                .id(material.getId())
                .name(material.getName())
                .unit(material.getMaterialUnit())
                .materialCode(material.getCode())
                .materialCategoryId(material.getCategoryId())
                .materialCategoryName(null) // 서비스 계층에서 할당
                .build();
    }

    public MaterialResponseDto withQuantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }

    public MaterialResponseDto withCategoryName(String categoryName) {
        this.materialCategoryName = categoryName;
        return this;
    }
}