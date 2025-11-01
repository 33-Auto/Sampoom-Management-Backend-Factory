package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialCategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialCategoryProjectionRepository extends JpaRepository<MaterialCategoryProjection, Long> {
    Optional<MaterialCategoryProjection> findByCategoryId(Long categoryId);

    // N+1 문제 해결을 위한 배치 조회 메서드
    List<MaterialCategoryProjection> findByCategoryIdIn(List<Long> categoryIds);
}
