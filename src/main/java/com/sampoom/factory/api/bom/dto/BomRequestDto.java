package com.sampoom.factory.api.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomRequestDto {
    private Long partId;
    private List<BomMaterialDto> materials;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BomMaterialDto {
        private Long materialId;
        private Long quantity;
    }
}