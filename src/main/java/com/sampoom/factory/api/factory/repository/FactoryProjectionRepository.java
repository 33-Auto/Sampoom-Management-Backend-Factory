package com.sampoom.factory.api.factory.repository;

import com.sampoom.factory.api.factory.entity.FactoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactoryProjectionRepository extends JpaRepository<FactoryProjection, Long> {
}
