package com.sampoom.factory.api.bom.repository;

import com.sampoom.factory.api.bom.entity.Bom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BomRepository extends JpaRepository<Bom,Long> {
    @Query("""
SELECT b FROM Bom b
JOIN b.part p
JOIN p.group g
JOIN g.category c
WHERE (
  COALESCE(:keyword, '') = ''  
  OR p.name ILIKE CONCAT('%', :keyword, '%')
  OR p.code ILIKE CONCAT('%', :keyword, '%')
)
AND (:categoryId IS NULL OR c.id = :categoryId)
AND (:groupId    IS NULL OR g.id = :groupId)
ORDER BY b.createdAt DESC
""")
    Page<Bom> findByFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("groupId") Long groupId,
            Pageable pageable);


}
