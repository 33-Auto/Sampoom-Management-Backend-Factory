package com.sampoom.factory.api.material.dto;

import com.sampoom.factory.api.material.entity.MaterialCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCategoryResponseDto {
    private Long id;
    private String name;
    private String code;

    public static MaterialCategoryResponseDto from(MaterialCategory category) {
        return MaterialCategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .build();
    }
}