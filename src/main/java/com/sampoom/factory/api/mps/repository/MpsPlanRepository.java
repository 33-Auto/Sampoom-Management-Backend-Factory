package com.sampoom.factory.api.mps.repository;

import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.entity.MpsPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MpsPlanRepository extends JpaRepository<MpsPlan, Long> {



    // MPS ID로 모든 계획 조회 (사이클 번호 순)
    List<MpsPlan> findByMpsMpsIdOrderByCycleNumber(Long mpsId);


    // MPS로 계획 삭제
    void deleteByMps(Mps mps);
}
