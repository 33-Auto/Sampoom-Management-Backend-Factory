package com.sampoom.factory.api.bom.repository;

import com.sampoom.factory.api.bom.entity.BomMaterialProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomMaterialProjectionRepository extends JpaRepository<BomMaterialProjection, Long> {
    void deleteByBomId(Long bomId);
}
