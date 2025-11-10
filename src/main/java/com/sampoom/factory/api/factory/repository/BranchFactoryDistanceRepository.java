package com.sampoom.factory.api.factory.repository;

import com.sampoom.factory.api.factory.entity.BranchFactoryDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchFactoryDistanceRepository extends JpaRepository<BranchFactoryDistance, Long> {

    Optional<BranchFactoryDistance> findByDistanceId(Long distanceId);

    List<BranchFactoryDistance> findByBranchName(String branchName);

    List<BranchFactoryDistance> findByFactoryId(Long factoryId);

    Optional<BranchFactoryDistance> findByBranchNameAndFactoryId(String branchName, Long factoryId);

    // 창고 ID 기반 조회 메서드
    List<BranchFactoryDistance> findByBranchId(Long branchId);

    // 창고 ID 기반으로 거리순 정렬 조회 (가장 가까운 공장부터)
    List<BranchFactoryDistance> findByBranchIdOrderByDistanceKmAsc(Long branchId);

    Optional<BranchFactoryDistance> findByBranchIdAndFactoryId(Long branchId, Long factoryId);
}
