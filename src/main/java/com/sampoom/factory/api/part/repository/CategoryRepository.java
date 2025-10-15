package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category,Long> {
}
