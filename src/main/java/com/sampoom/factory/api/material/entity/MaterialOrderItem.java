package com.sampoom.factory.api.material.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "material_order_item")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_order_item_id")
    private Long id;

    private Long quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_order_id")
    private MaterialOrder materialOrder;

    @Column(name = "material_id", nullable = false)
    private Long materialId;   // 실제 DB에 저장되는 FK 값

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private Material material; // 읽기 전용 뷰


}