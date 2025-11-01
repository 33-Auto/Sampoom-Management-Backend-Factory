package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartOrderRepository extends JpaRepository<PartOrder,Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<PartOrder> findByIdAndFactoryId(Long id, Long factoryId);


    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryId(Long factoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatus(Long factoryId, PartOrderStatus status, Pageable pageable);

    // 주문 코드 생성을 위한 메서드 추가
    @Query("SELECT p.orderCode FROM PartOrder p WHERE p.orderCode LIKE CONCAT('WO-', :year, '-%') ORDER BY p.orderCode DESC LIMIT 1")
    String findLastOrderCodeByYear(@Param("year") String year);
}
