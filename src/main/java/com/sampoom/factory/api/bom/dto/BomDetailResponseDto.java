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
public class BomDetailResponseDto {
    private Long id;
    private String partName;
    private String partCode;
    private Long partId;
    private List<BomMaterialDto> materials;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BomMaterialDto {
        private Long id;
        private Long materialId;
        private String materialName;
        private String materialCode;
        private Long quantity;
    }

    public static BomDetailResponseDto from(Bom bom) {
        List<BomMaterialDto> materialDtos = bom.getMaterials().stream()
                .map(material -> BomMaterialDto.builder()
                        .id(material.getId())
                        .materialId(material.getMaterial().getId())
                        .materialName(material.getMaterial().getName())
                        .materialCode(material.getMaterial().getCode())
                        .quantity(material.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return BomDetailResponseDto.builder()
                .id(bom.getId())
                .partId(bom.getPart().getId())
                .partName(bom.getPart().getName())
                .partCode(bom.getPart().getCode())
                .materials(materialDtos)
                .createdAt(bom.getCreatedAt())
                .updatedAt(bom.getUpdatedAt())
                .build();
    }
}
