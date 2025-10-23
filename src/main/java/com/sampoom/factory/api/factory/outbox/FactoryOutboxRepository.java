package com.sampoom.factory.api.factory.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface FactoryOutboxRepository extends JpaRepository<FactoryOutbox, Long> {

    @Query(value = """
        SELECT *
        FROM factory_outbox
        WHERE status IN ('READY','FAILED')
          AND retry_count < :maxRetry
          AND (next_retry_at IS NULL OR next_retry_at <= now())
        ORDER BY occurred_at ASC
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
        """, nativeQuery = true)
    List<FactoryOutbox> pickReadyBatch(@Param("limit") int limit,
                                       @Param("maxRetry") int maxRetry);
}
