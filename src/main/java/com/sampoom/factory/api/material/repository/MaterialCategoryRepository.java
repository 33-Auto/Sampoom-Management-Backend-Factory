package com.sampoom.factory.api.material.repository;

import com.sampoom.factory.api.material.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory,Long> {
}
