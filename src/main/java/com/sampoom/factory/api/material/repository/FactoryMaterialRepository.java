package com.sampoom.factory.api.material.repository;


import com.sampoom.factory.api.material.entity.FactoryMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FactoryMaterialRepository extends JpaRepository<FactoryMaterial, Long> {



    @EntityGraph(attributePaths = {"material", "material.materialCategory"})
    Page<FactoryMaterial> findByFactory_IdAndMaterial_MaterialCategory_Id(
            Long factoryId, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"material"})
    Page<FactoryMaterial> findByFactory_Id(Long factoryId, Pageable pageable);

    Optional<FactoryMaterial> findByFactoryIdAndMaterialId(Long factoryId, Long materialId);
}