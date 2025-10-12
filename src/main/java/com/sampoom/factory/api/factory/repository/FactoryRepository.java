package com.sampoom.factory.api.factory.repository;

import com.sampoom.factory.api.factory.entity.Factory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<Factory,Long> {
}
