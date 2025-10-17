package com.sampoom.factory.api.bom.repository;

import com.sampoom.factory.api.bom.entity.BomMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomMaterialRepository extends JpaRepository<BomMaterial,Long> {
}
