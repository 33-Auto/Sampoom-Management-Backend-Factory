package com.sampoom.factory.api.material.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long id;

    @Column(name = "material_name", nullable = false)
    private String name;

    @Column(name = "material_code", nullable = false)
    private String code;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays; // 자재 조달 리드타임 (일 단위)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_category_id")
    private MaterialCategory materialCategory;
}