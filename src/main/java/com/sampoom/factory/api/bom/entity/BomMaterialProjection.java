package com.sampoom.factory.api.bom.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bom_material_projection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BomMaterialProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bomId; // FK 대신 ID만 저장

    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private String materialName;

    @Column(nullable = false)
    private String materialCode;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private Integer quantity;

    public BomMaterialProjection updateFromEvent(String materialName, String materialCode, String unit, Integer quantity) {
        return this.toBuilder()
                .materialName(materialName)
                .materialCode(materialCode)
                .unit(unit)
                .quantity(quantity)
                .build();
    }
}