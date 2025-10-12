package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialOrderItemRepository extends JpaRepository<MaterialOrderItem,Long> {
    List<MaterialOrderItem> findByMaterialOrderId(Long materialOrderId);
}
