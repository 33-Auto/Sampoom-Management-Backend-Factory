package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialProjectionRepository extends JpaRepository<MaterialProjection, Long> {

    Optional<MaterialProjection> findByMaterialId(Long materialId);

    Optional<MaterialProjection> findByCode(String code);

    // N+1 문제 해결을 위한 배치 조회 메서드
    List<MaterialProjection> findByMaterialIdIn(List<Long> materialIds);
}
