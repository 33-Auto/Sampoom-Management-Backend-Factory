package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialCategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialCategoryProjectionRepository extends JpaRepository<MaterialCategoryProjection, Long> {
    Optional<MaterialCategoryProjection> findByCategoryId(Long categoryId);
}
