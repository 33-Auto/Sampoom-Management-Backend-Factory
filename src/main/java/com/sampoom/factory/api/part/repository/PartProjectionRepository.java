package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartProjectionRepository extends JpaRepository<PartProjection, Long> {

    Optional<PartProjection> findByPartId(Long partId);
}
