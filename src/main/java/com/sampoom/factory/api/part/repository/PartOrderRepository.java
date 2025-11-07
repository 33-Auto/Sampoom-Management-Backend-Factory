package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderPriority;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartOrderRepository extends JpaRepository<PartOrder, Long> {

    // 기본 조회 메서드들
    Page<PartOrder> findByFactoryId(Long factoryId, Pageable pageable);

    Page<PartOrder> findByFactoryIdAndStatus(Long factoryId, PartOrderStatus status, Pageable pageable);

    Page<PartOrder> findByFactoryIdAndStatusIn(Long factoryId, List<PartOrderStatus> statuses, Pageable pageable);

    Page<PartOrder> findByFactoryIdAndPriorityIn(Long factoryId, List<PartOrderPriority> priorities, Pageable pageable);

    Page<PartOrder> findByFactoryIdAndStatusInAndPriorityIn(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities, Pageable pageable);

    Optional<PartOrder> findByIdAndFactoryId(Long id, Long factoryId);

    // 비관적 락을 사용한 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PartOrder po WHERE po.id = :id AND po.factoryId = :factoryId")
    Optional<PartOrder> findByIdAndFactoryIdWithLock(@Param("id") Long id, @Param("factoryId") Long factoryId);

    // 검색 기능이 포함된 메서드들
    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findByFactoryIdWithSearch(@Param("factoryId") Long factoryId,
                                             @Param("query") String query,
                                             Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.status IN :statuses " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findByFactoryIdAndStatusInWithSearch(@Param("factoryId") Long factoryId,
                                                        @Param("statuses") List<PartOrderStatus> statuses,
                                                        @Param("query") String query,
                                                        Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.priority IN :priorities " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findByFactoryIdAndPriorityInWithSearch(@Param("factoryId") Long factoryId,
                                                          @Param("priorities") List<PartOrderPriority> priorities,
                                                          @Param("query") String query,
                                                          Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.status IN :statuses " +
           "AND po.priority IN :priorities " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findByFactoryIdAndStatusInAndPriorityInWithSearch(@Param("factoryId") Long factoryId,
                                                                     @Param("statuses") List<PartOrderStatus> statuses,
                                                                     @Param("priorities") List<PartOrderPriority> priorities,
                                                                     @Param("query") String query,
                                                                     Pageable pageable);

    // 카테고리, 그룹 필터링이 포함된 메서드들
    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (:statuses IS NULL OR po.status IN :statuses) " +
           "AND (:priorities IS NULL OR po.priority IN :priorities) " +
           "AND (:categoryId IS NULL OR pp.categoryId = :categoryId) " +
           "AND (:groupId IS NULL OR pp.groupId = :groupId)")
    Page<PartOrder> findByFactoryIdWithFilters(@Param("factoryId") Long factoryId,
                                              @Param("statuses") List<PartOrderStatus> statuses,
                                              @Param("priorities") List<PartOrderPriority> priorities,
                                              @Param("categoryId") Long categoryId,
                                              @Param("groupId") Long groupId,
                                              Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (:statuses IS NULL OR po.status IN :statuses) " +
           "AND (:priorities IS NULL OR po.priority IN :priorities) " +
           "AND (:categoryId IS NULL OR pp.categoryId = :categoryId) " +
           "AND (:groupId IS NULL OR pp.groupId = :groupId) " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findByFactoryIdWithFiltersAndSearch(@Param("factoryId") Long factoryId,
                                                       @Param("statuses") List<PartOrderStatus> statuses,
                                                       @Param("priorities") List<PartOrderPriority> priorities,
                                                       @Param("categoryId") Long categoryId,
                                                       @Param("groupId") Long groupId,
                                                       @Param("query") String query,
                                                       Pageable pageable);

    // 생산계획용 메서드들
    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :planStatuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate)) " +
           "AND (:priorities IS NULL OR po.priority IN :priorities) " +
           "AND (:categoryId IS NULL OR pp.categoryId = :categoryId) " +
           "AND (:groupId IS NULL OR pp.groupId = :groupId)")
    Page<PartOrder> findProductionPlansWithFilters(@Param("factoryId") Long factoryId,
                                                  @Param("planStatuses") List<PartOrderStatus> planStatuses,
                                                  @Param("priorities") List<PartOrderPriority> priorities,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("groupId") Long groupId,
                                                  @Param("cutoffDate") LocalDateTime cutoffDate,
                                                  Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :planStatuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate)) " +
           "AND (:priorities IS NULL OR po.priority IN :priorities) " +
           "AND (:categoryId IS NULL OR pp.categoryId = :categoryId) " +
           "AND (:groupId IS NULL OR pp.groupId = :groupId) " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findProductionPlansWithFiltersAndSearch(@Param("factoryId") Long factoryId,
                                                           @Param("planStatuses") List<PartOrderStatus> planStatuses,
                                                           @Param("priorities") List<PartOrderPriority> priorities,
                                                           @Param("categoryId") Long categoryId,
                                                           @Param("groupId") Long groupId,
                                                           @Param("query") String query,
                                                           @Param("cutoffDate") LocalDateTime cutoffDate,
                                                           Pageable pageable);

    // 호환성을 위한 기존 메서드들
    @Query("SELECT po FROM PartOrder po WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :statuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate)) " +
           "AND (:priorities IS NULL OR po.priority IN :priorities)")
    Page<PartOrder> findProductionPlans(@Param("factoryId") Long factoryId,
                                       @Param("statuses") List<PartOrderStatus> statuses,
                                       @Param("priorities") List<PartOrderPriority> priorities,
                                       @Param("cutoffDate") LocalDateTime cutoffDate,
                                       Pageable pageable);

    @Query("SELECT po FROM PartOrder po WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :statuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate))")
    Page<PartOrder> findProductionPlans(@Param("factoryId") Long factoryId,
                                       @Param("statuses") List<PartOrderStatus> statuses,
                                       @Param("cutoffDate") LocalDateTime cutoffDate,
                                       Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :statuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate)) " +
           "AND po.priority IN :priorities " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findProductionPlansWithSearch(@Param("factoryId") Long factoryId,
                                                  @Param("statuses") List<PartOrderStatus> statuses,
                                                  @Param("priorities") List<PartOrderPriority> priorities,
                                                  @Param("query") String query,
                                                  @Param("cutoffDate") LocalDateTime cutoffDate,
                                                  Pageable pageable);

    @Query("SELECT DISTINCT po FROM PartOrder po JOIN po.items poi JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (po.status IN :statuses OR (po.status = 'IN_PROGRESS' AND po.updatedAt >= :cutoffDate)) " +
           "AND (pp.name LIKE :query OR pp.code LIKE :query OR po.orderCode LIKE :query)")
    Page<PartOrder> findProductionPlansWithSearch(@Param("factoryId") Long factoryId,
                                                  @Param("statuses") List<PartOrderStatus> statuses,
                                                  @Param("query") String query,
                                                  @Param("cutoffDate") LocalDateTime cutoffDate,
                                                  Pageable pageable);

    // 스케줄러 및 코드 생성기용 메서드들 추가
    @Query(value = "SELECT * FROM part_order WHERE order_code LIKE CONCAT(:prefix, '%') ORDER BY order_code DESC LIMIT 1", nativeQuery = true)
    Optional<PartOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(@Param("prefix") String prefix);

    List<PartOrder> findByStatus(PartOrderStatus status);

    List<PartOrder> findByStatusAndScheduledDateBefore(PartOrderStatus status, LocalDateTime scheduledDate);
}
