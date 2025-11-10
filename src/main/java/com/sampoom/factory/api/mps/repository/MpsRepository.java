package com.sampoom.factory.api.mps.repository;

import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MpsRepository extends JpaRepository<Mps, Long> {

    // 부품 ID로 MPS 조회
    List<Mps> findByPartId(Long partId);

    // 상태별 MPS 조회
    List<Mps> findByStatus(MpsStatus status);

    // 부품ID, 예측달, 창고ID로 MPS 조회
    @Query("SELECT m FROM Mps m WHERE m.partId = :partId AND m.warehouseId = :warehouseId AND YEAR(m.targetDate) = :year AND MONTH(m.targetDate) = :month")
    Optional<Mps> findByPartIdAndWarehouseIdAndForecastMonth(@Param("partId") Long partId,
                                                              @Param("warehouseId") Long warehouseId,
                                                              @Param("year") int year,
                                                              @Param("month") int month);

    // 공장ID, 부품ID, 예측달, 창고ID로 MPS 조회
    @Query("SELECT m FROM Mps m WHERE m.factoryId = :factoryId AND m.partId = :partId AND m.warehouseId = :warehouseId AND YEAR(m.targetDate) = :year AND MONTH(m.targetDate) = :month")
    Optional<Mps> findByFactoryIdAndPartIdAndWarehouseIdAndForecastMonth(@Param("factoryId") Long factoryId,
                                                                          @Param("partId") Long partId,
                                                                          @Param("warehouseId") Long warehouseId,
                                                                          @Param("year") int year,
                                                                          @Param("month") int month);

    // 특정 날짜 범위의 MPS 조회
    @Query("SELECT m FROM Mps m WHERE m.startDate BETWEEN :startDate AND :endDate")
    List<Mps> findByStartDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 목표 날짜별 MPS 조회
    List<Mps> findByTargetDate(LocalDate targetDate);

    // 부품별 최신 MPS 조회
    @Query("SELECT m FROM Mps m WHERE m.partId = :partId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Mps> findLatestByPartId(@Param("partId") Long partId);

    // 시작일이 특정 날짜 이전인 MPS 조회 (긴급 생산 필요)
    @Query("SELECT m FROM Mps m WHERE m.startDate <= :date AND m.status = :status")
    List<Mps> findUrgentMps(@Param("date") LocalDate date, @Param("status") MpsStatus status);
}

