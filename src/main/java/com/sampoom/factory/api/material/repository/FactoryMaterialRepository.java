package com.sampoom.factory.api.material.repository;


import com.sampoom.factory.api.material.entity.FactoryMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FactoryMaterialRepository extends JpaRepository<FactoryMaterial, Long> {

    Optional<FactoryMaterial> findByFactoryIdAndMaterialId(Long factoryId, Long materialId);

    Optional<FactoryMaterial> findFirstByFactoryIdAndMaterialId(Long factoryId, Long materialId);

    Page<FactoryMaterial> findByFactoryIdAndMaterialIdIn(Long factoryId, Iterable<Long> materialIds, Pageable pageable);

    @Query("""
        select fm
        from FactoryMaterial fm
        where fm.factoryId = :factoryId
          and (:categoryId is null or fm.materialId in (
            select mp.materialId from MaterialProjection mp where mp.categoryId = :categoryId
          ))
        """)
    Page<FactoryMaterial> findByFactoryAndCategory(
            @Param("factoryId") Long factoryId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("""
        select fm
        from FactoryMaterial fm
        where fm.factoryId = :factoryId
          and (:categoryId is null or fm.materialId in (
            select mp.materialId from MaterialProjection mp where mp.categoryId = :categoryId
          ))
          and (:keyword is null or :keyword = '' or fm.materialId in (
            select mp.materialId from MaterialProjection mp where mp.name like concat('%', :keyword, '%')
          ))
        """)
    Page<FactoryMaterial> findByFactoryCategoryAndKeyword(
            @Param("factoryId") Long factoryId,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    void deleteAllByFactoryId(Long factoryId);
}