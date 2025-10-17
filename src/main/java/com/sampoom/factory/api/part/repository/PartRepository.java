package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part,Long> {
}
