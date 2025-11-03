package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PartOrderRepository extends JpaRepository<PartOrder,Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<PartOrder> findByIdAndFactoryId(Long id, Long factoryId);

    // 비관적 락을 사용하는 조회 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT po FROM PartOrder po WHERE po.id = :id AND po.factory.id = :factoryId")
    Optional<PartOrder> findByIdAndFactoryIdWithLock(@Param("id") Long id, @Param("factoryId") Long factoryId);


    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryId(Long factoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatus(Long factoryId, PartOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatusIn(Long factoryId, List<PartOrderStatus> statuses, Pageable pageable);

    Optional<PartOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(String orderCodePrefix);
}
