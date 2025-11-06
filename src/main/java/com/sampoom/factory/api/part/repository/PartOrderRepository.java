package com.sampoom.factory.api.part.repository;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderPriority;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartOrderRepository extends JpaRepository<PartOrder,Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<PartOrder> findByIdAndFactoryId(Long id, Long factoryId);

    // 비관적 락을 사용하는 조회 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT po FROM PartOrder po WHERE po.id = :id AND po.factoryId = :factoryId")
    Optional<PartOrder> findByIdAndFactoryIdWithLock(@Param("id") Long id, @Param("factoryId") Long factoryId);


    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryId(Long factoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatus(Long factoryId, PartOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatusIn(Long factoryId, List<PartOrderStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndPriorityIn(Long factoryId, List<PartOrderPriority> priorities, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Page<PartOrder> findByFactoryIdAndStatusInAndPriorityIn(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities, Pageable pageable);

    // 검색 기능 지원 메서드들
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT DISTINCT po FROM PartOrder po " +
           "LEFT JOIN po.items poi " +
           "LEFT JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND (LOWER(po.orderCode) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.name) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.code) LIKE LOWER(:searchQuery))")
    Page<PartOrder> findByFactoryIdWithSearch(@Param("factoryId") Long factoryId,
                                              @Param("searchQuery") String searchQuery,
                                              Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT DISTINCT po FROM PartOrder po " +
           "LEFT JOIN po.items poi " +
           "LEFT JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.status IN :statuses " +
           "AND (LOWER(po.orderCode) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.name) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.code) LIKE LOWER(:searchQuery))")
    Page<PartOrder> findByFactoryIdAndStatusInWithSearch(@Param("factoryId") Long factoryId,
                                                         @Param("statuses") List<PartOrderStatus> statuses,
                                                         @Param("searchQuery") String searchQuery,
                                                         Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT DISTINCT po FROM PartOrder po " +
           "LEFT JOIN po.items poi " +
           "LEFT JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.priority IN :priorities " +
           "AND (LOWER(po.orderCode) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.name) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.code) LIKE LOWER(:searchQuery))")
    Page<PartOrder> findByFactoryIdAndPriorityInWithSearch(@Param("factoryId") Long factoryId,
                                                           @Param("priorities") List<PartOrderPriority> priorities,
                                                           @Param("searchQuery") String searchQuery,
                                                           Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT DISTINCT po FROM PartOrder po " +
           "LEFT JOIN po.items poi " +
           "LEFT JOIN PartProjection pp ON poi.partId = pp.partId " +
           "WHERE po.factoryId = :factoryId " +
           "AND po.status IN :statuses " +
           "AND po.priority IN :priorities " +
           "AND (LOWER(po.orderCode) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.name) LIKE LOWER(:searchQuery) " +
           "OR LOWER(pp.code) LIKE LOWER(:searchQuery))")
    Page<PartOrder> findByFactoryIdAndStatusInAndPriorityInWithSearch(@Param("factoryId") Long factoryId,
                                                                      @Param("statuses") List<PartOrderStatus> statuses,
                                                                      @Param("priorities") List<PartOrderPriority> priorities,
                                                                      @Param("searchQuery") String searchQuery,
                                                                      Pageable pageable);

    Optional<PartOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(String orderCodePrefix);

    // 스케줄러에서 사용할 메서드들

    // 특정 상태의 주문들 조회
    List<PartOrder> findByStatus(PartOrderStatus status);

    // 특정 상태이면서 예정일이 지난 주문들 조회
    List<PartOrder> findByStatusAndScheduledDateBefore(PartOrderStatus status, LocalDateTime scheduledDate);

    // 예정일이 지난 진행중인 주문들 조회
    @Query("SELECT po FROM PartOrder po WHERE po.status = :status AND po.scheduledDate < :currentTime")
    List<PartOrder> findOverdueOrdersByStatus(@Param("status") PartOrderStatus status, @Param("currentTime") LocalDateTime currentTime);
}
