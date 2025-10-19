package com.sampoom.factory.api.bom.entity;

import com.sampoom.factory.api.material.entity.Material;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bom_material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BomMaterial {

    @Id
    @Column(name = "bom_material_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private Long quantity;

    public void updateBom(Bom bom) {
        this.bom = bom;
    }



}