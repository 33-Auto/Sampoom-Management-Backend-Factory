package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartGroupProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartGroupProjectionRepository extends JpaRepository<PartGroupProjection, Long> {

    Optional<PartGroupProjection> findByGroupId(Long groupId);
}
