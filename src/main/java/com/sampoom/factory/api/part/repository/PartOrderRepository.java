package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartOrderRepository extends JpaRepository<PartOrder,Long> {

    @EntityGraph(attributePaths = {"items", "items.part", "items.part.group", "items.part.group.category"})
    Optional<PartOrder> findByIdAndFactoryId(Long id, Long factoryId);


    @EntityGraph(attributePaths = {"items", "items.part", "items.part.group", "items.part.group.category"})
    Page<PartOrder> findByFactoryId(Long factoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.part", "items.part.group", "items.part.group.category"})
    Page<PartOrder> findByFactoryIdAndStatus(Long factoryId, PartOrderStatus status, Pageable pageable);
}
