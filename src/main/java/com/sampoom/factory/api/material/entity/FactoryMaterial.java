package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.api.factory.entity.Factory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "factory_material")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactoryMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

    @Column(name = "material_id", nullable = false)
    private Long materialId;   // 실제 DB에 저장되는 FK 값

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private Material material; // 읽기 전용 뷰

    private Long quantity;

    public void increaseQuantity(Long amount) {
        this.quantity += amount;
    }
}
