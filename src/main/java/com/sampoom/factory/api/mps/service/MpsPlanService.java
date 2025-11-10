package com.sampoom.factory.api.mps.service;

import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.entity.MpsPlanStatus;
import com.sampoom.factory.api.mps.repository.MpsPlanRepository;
import com.sampoom.factory.api.mps.repository.MpsRepository;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.service.PartOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MpsPlanService {

    private final MpsPlanRepository mpsPlanRepository;
    private final MpsRepository mpsRepository;
    private final PartOrderService partOrderService; // 추가된 필드

    /**
     * MpsPlan 상세 조회
     */
    public MpsPlan getMpsPlanById(Long mpsPlanId) {
        return mpsPlanRepository.findById(mpsPlanId)
                .orElseThrow(() -> new IllegalArgumentException("MpsPlan을 찾을 수 없습니다: " + mpsPlanId));
    }

    /**
     * MPS ID로 해당하는 MpsPlan 목록 조회
     */
    public List<MpsPlan> getMpsPlansByMpsId(Long mpsId) {
        log.info("MpsPlan 목록 조회 - mpsId: {}", mpsId);
        List<MpsPlan> mpsPlans = mpsPlanRepository.findByMpsMpsIdOrderByCycleNumber(mpsId);
        log.info("MpsPlan 목록 조회 완료 - mpsId: {}, 개수: {}", mpsId, mpsPlans.size());
        return mpsPlans;
    }

    /**
     * MPS 실행 - MPS 기반으로 MpsPlan들을 생성
     */
    @Transactional
    public List<MpsPlan> executeMps(Mps mps) {
        // 기존 MpsPlan이 있다면 삭제
        mpsPlanRepository.deleteByMps(mps);

        List<MpsPlan> mpsPlans = new ArrayList<>();

        if (mps.getProductionCycles() == 0) {
            log.info("생산 회수가 0이므로 MpsPlan을 생성하지 않습니다. mpsId: {}", mps.getMpsId());
            return mpsPlans;
        }

        // 각 생산 회차별로 MpsPlan 생성
        int remainingProduction = mps.getTotalProduction();

        for (int cycle = 1; cycle <= mps.getProductionCycles(); cycle++) {
            // 각 회차별 생산량 계산 (마지막 회차는 남은 수량 모두)
            int cycleProductionQuantity;
            if (cycle == mps.getProductionCycles()) {
                cycleProductionQuantity = remainingProduction;
            } else {
                cycleProductionQuantity = mps.getStandardQuantity();
            }

            // 완료 요구일 계산 (시작일 + (회차-1) * 리드타임)
            LocalDate requiredDate = mps.getStartDate().plusDays((cycle - 1) * mps.getLeadTime());

            // 남은 총 생산량 계산
            remainingProduction -= cycleProductionQuantity;

            MpsPlan mpsPlan = MpsPlan.builder()
                    .mps(mps)  // 직접 연관관계 설정
                    .cycleNumber(cycle)
                    .requiredDate(requiredDate)
                    .productionQuantity(cycleProductionQuantity)
                    .remainingTotalProduction(remainingProduction)
                    .status(MpsPlanStatus.PLANNED)
                    .build();

            mpsPlans.add(mpsPlan);

            log.debug("MpsPlan 생성 - mpsId: {}, cycle: {}, 요구일: {}, 생산량: {}, 남은량: {}",
                    mps.getMpsId(), cycle, requiredDate, cycleProductionQuantity, remainingProduction);
        }

        List<MpsPlan> savedPlans = mpsPlanRepository.saveAll(mpsPlans);
        log.info("MPS 실행 완료 - mpsId: {}, 총 {}개 계획 생성", mps.getMpsId(), savedPlans.size());

        return savedPlans;
    }

    /**
     * MpsPlan들을 기반으로 PartOrder 생성
     */
    @Transactional
    public List<PartOrderResponseDto> createPartOrdersFromMpsPlans(List<MpsPlan> mpsPlans) {
        log.info("MpsPlan들로부터 PartOrder 생성 시작 - 계획 수: {}", mpsPlans.size());

        List<PartOrderResponseDto> partOrderResponses = new ArrayList<>();

        for (MpsPlan mpsPlan : mpsPlans) {
            try {
                Mps mps = mpsPlan.getMps();

                // MPS 주문용 PartOrder 생성
                PartOrderResponseDto partOrder = partOrderService.createMpsPartOrder(
                        mps.getFactoryId(),
                        mps.getWarehouseId(),
                        mps.getPartId(),
                        mpsPlan.getProductionQuantity().longValue(),
                        mpsPlan.getRequiredDate().atStartOfDay(),
                        mpsPlan.getMpsPlanId(),
                        "MPS-" + mps.getMpsId() + "-" + mpsPlan.getCycleNumber()
                );

                partOrderResponses.add(partOrder);

                log.info("MpsPlan에서 PartOrder 생성 성공 - mpsPlanId: {}, partOrderId: {}, 생산량: {}",
                        mpsPlan.getMpsPlanId(), partOrder.getOrderId(), mpsPlan.getProductionQuantity());

            } catch (Exception e) {
                log.error("MpsPlan에서 PartOrder 생성 실패 - mpsPlanId: {}, 오류: {}",
                        mpsPlan.getMpsPlanId(), e.getMessage(), e);
                // 개별 실패는 전체 작업을 중단하지 않고 계속 진행
            }
        }

        log.info("MpsPlan들로부터 PartOrder 생성 완료 - 성공: {}/{}",
                partOrderResponses.size(), mpsPlans.size());

        return partOrderResponses;
    }

    // ...existing code...
}
