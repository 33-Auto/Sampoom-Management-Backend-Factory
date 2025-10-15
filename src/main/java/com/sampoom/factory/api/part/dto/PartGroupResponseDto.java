package com.sampoom.factory.api.part.dto;

import com.sampoom.factory.api.part.entity.PartGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartGroupResponseDto {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;

    public static PartGroupResponseDto from(PartGroup partGroup) {
        return PartGroupResponseDto.builder()
                .id(partGroup.getId())
                .code(partGroup.getCode())
                .name(partGroup.getName())
                .categoryId(partGroup.getCategory().getId())
                .categoryName(partGroup.getCategory().getName())
                .build();
    }
}