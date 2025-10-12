package com.sampoom.factory.api.material.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "material_category")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_category_id")
    private Long id;

    @Column(name = "material_category_name")
    private String name;

    @Column(name = "material_category_code")
    private String code;
}