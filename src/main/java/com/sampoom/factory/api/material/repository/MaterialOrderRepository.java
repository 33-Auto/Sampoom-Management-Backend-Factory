package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialOrderRepository extends JpaRepository<MaterialOrder,Long> {
    Page<MaterialOrder> findByFactoryId(Long factoryId, Pageable pageable);

    Optional<MaterialOrder> findByIdAndFactory_Id(Long orderId, Long factoryId);
}
