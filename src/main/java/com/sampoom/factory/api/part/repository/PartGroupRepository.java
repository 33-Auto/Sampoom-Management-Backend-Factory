package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.Category;
import com.sampoom.factory.api.part.entity.PartGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartGroupRepository extends JpaRepository<PartGroup, Long> {
    List<PartGroup> findByCategory(Category category);
}
