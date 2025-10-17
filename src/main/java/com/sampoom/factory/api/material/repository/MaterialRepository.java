package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material,Long> {
}
