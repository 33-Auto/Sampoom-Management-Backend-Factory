package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartCategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartCategoryProjectionRepository extends JpaRepository<PartCategoryProjection, Long> {

    Optional<PartCategoryProjection> findByCategoryId(Long categoryId);
}
