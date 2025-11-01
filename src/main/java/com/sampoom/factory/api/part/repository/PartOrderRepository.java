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

    Optional<PartOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(String orderCodePrefix);
}
