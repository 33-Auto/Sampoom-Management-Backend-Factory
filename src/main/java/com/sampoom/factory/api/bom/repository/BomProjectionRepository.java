package com.sampoom.factory.api.bom.repository;

import com.sampoom.factory.api.bom.entity.BomProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BomProjectionRepository extends JpaRepository<BomProjection, Long> {
    Optional<BomProjection> findByBomId(Long bomId);
}
