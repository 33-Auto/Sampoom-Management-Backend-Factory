package com.sampoom.factory.api.bom.dto;


import com.sampoom.factory.api.bom.entity.Bom;
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
public class BomResponseDto {
    private Long id;
    private String partName;
    private String partCode;
    private Long partId;
    private List<BomMaterialDto> materials;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BomResponseDto from(Bom bom) {
        return BomResponseDto.builder()
                .id(bom.getId())
                .partId(bom.getPart().getId())
                .partName(bom.getPart().getName())
                .partCode(bom.getPart().getCode())
                .materials(bom.getMaterials().stream()
                        .map(BomMaterialDto::from)
                        .collect(Collectors.toList()))
                .createdAt(bom.getCreatedAt())
                .updatedAt(bom.getUpdatedAt())
                .build();
    }
}