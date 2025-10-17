package com.sampoom.factory.api.material.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "material_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_category_id")
    private Long id;

    @Column(name = "material_category_name", nullable = false)
    private String name;

    @Column(name = "material_category_code", nullable = false)
    private String code;
}