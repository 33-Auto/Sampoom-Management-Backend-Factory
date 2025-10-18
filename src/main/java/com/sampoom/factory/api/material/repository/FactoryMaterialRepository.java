package com.sampoom.factory.api.material.repository;


import com.sampoom.factory.api.material.entity.FactoryMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FactoryMaterialRepository extends JpaRepository<FactoryMaterial, Long> {



    @EntityGraph(attributePaths = {"material", "material.materialCategory"})
    Page<FactoryMaterial> findByFactory_IdAndMaterial_MaterialCategory_Id(
            Long factoryId, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"material"})
    Page<FactoryMaterial> findByFactory_Id(Long factoryId, Pageable pageable);

    Optional<FactoryMaterial> findByFactoryIdAndMaterialId(Long factoryId, Long materialId);



    @EntityGraph(attributePaths = {"material", "material.materialCategory"})
    @Query("""
        select fm
        from FactoryMaterial fm
        join fm.factory factory
        join fm.material material
        left join material.materialCategory category
        where factory.id = :factoryId
          and (:categoryId is null or category.id = :categoryId)
        """)
    Page<FactoryMaterial> findByFactoryAndCategory(
            @Param("factoryId") Long factoryId,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"material", "material.materialCategory"})
    @Query("""
        select fm
        from FactoryMaterial fm
        join fm.factory factory
        join fm.material material
        left join material.materialCategory category
        where factory.id = :factoryId
          and (:categoryId is null or category.id = :categoryId)
          and (lower(material.name) like lower(concat('%', :keyword, '%'))
               or lower(material.code) like lower(concat('%', :keyword, '%')))
        """)
    Page<FactoryMaterial> findByFactoryCategoryAndKeyword(
            @Param("factoryId") Long factoryId,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}