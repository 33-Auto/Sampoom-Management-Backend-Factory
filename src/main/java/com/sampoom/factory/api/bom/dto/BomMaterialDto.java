package com.sampoom.factory.api.bom.dto;

import com.sampoom.factory.api.bom.entity.BomMaterial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomMaterialDto {
    private Long id;              // BomMaterial 엔티티의 ID
    private Long materialId;      // Material 엔티티의 ID
    private String materialName;  // 자재명
    private String materialCode;  // 자재 코드
    private String unit;          // 단위
    private Long quantity;         // 수량

    public static BomMaterialDto from(BomMaterial material) {
        return BomMaterialDto.builder()
                .id(material.getId())
                .materialId(material.getMaterial().getId())
                .materialName(material.getMaterial().getName())
                .materialCode(material.getMaterial().getCode())
                .unit(material.getMaterial().getUnit())
                .quantity(material.getQuantity())
                .build();
    }
}