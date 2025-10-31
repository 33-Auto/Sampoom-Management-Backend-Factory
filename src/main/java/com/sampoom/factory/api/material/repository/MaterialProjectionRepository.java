package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialProjectionRepository extends JpaRepository<MaterialProjection, Long> {

    Optional<MaterialProjection> findByMaterialId(Long materialId);
}
