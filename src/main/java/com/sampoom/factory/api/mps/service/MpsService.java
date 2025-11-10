package com.sampoom.factory.api.mps.service;

import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.repository.MpsRepository;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MpsService {

    private final MpsRepository mpsRepository;
    private final MpsPlanService mpsPlanService;

    /**
     * 부품ID, 예측달, 창고ID로 MPS 조회
     */
    public Optional<Mps> getMpsByPartAndForecastAndWarehouse(Long partId, LocalDate forecastMonth, Long warehouseId) {
        int year = forecastMonth.getYear();
        int month = forecastMonth.getMonthValue();

        return mpsRepository.findByPartIdAndWarehouseIdAndForecastMonth(partId, warehouseId, year, month);
    }

    /**
     * 공장ID, 부품ID, 예측달, 창고ID로 MPS 조회
     */
    public Mps getMpsByFactoryAndPartAndForecastAndWarehouse(Long factoryId, Long partId, LocalDate forecastMonth, Long warehouseId) {
        // 예측달에서 1달을 뺀 날짜 계산 (예: 2025-12 → 2025-11, 2025-01 → 2024-12)
        LocalDate targetMonth = forecastMonth.minusMonths(1);
        int year = targetMonth.getYear();
        int month = targetMonth.getMonthValue();

        log.info("MPS 조회 시작 - factoryId: {}, partId: {}, 예측달: {}, 실제조회달: {}-{}, warehouseId: {}",
                factoryId, partId, forecastMonth, year, month, warehouseId);

        Optional<Mps> result = mpsRepository.findByFactoryIdAndPartIdAndWarehouseIdAndForecastMonth(factoryId, partId, warehouseId, year, month);

        return result.orElseThrow(() -> {
            log.warn("MPS를 찾을 수 없습니다 - factoryId: {}, partId: {}, 실제조회달: {}-{}, warehouseId: {}",
                    factoryId, partId, year, month, warehouseId);
            return new NotFoundException(ErrorStatus.MPS_NOT_FOUND);
        });
    }

    /**
     * MPS 실행 - MpsPlan 생성
     */
    @Transactional
    public List<MpsPlan> executeMps(Long mpsId) {
        Mps mps = mpsRepository.findById(mpsId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MPS_NOT_FOUND));

        log.info("MPS 실행 시작 - mpsId: {}", mpsId);

        List<MpsPlan> mpsPlans = mpsPlanService.executeMps(mps);

        log.info("MPS 실행 완료 - mpsId: {}, 생성된 계획 수: {}", mpsId, mpsPlans.size());

        return mpsPlans;
    }

    /**
     * MPS ID로 해당하는 MpsPlan 목록 조회
     */
    public List<MpsPlan> getMpsPlansByMpsId(Long mpsId) {
        log.info("MpsPlan 목록 조회 시작 - mpsId: {}", mpsId);

        // MPS 존재 여부 확인
        mpsRepository.findById(mpsId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MPS_NOT_FOUND));

        List<MpsPlan> mpsPlans = mpsPlanService.getMpsPlansByMpsId(mpsId);

        log.info("MpsPlan 목록 조회 완료 - mpsId: {}, 계획 수: {}", mpsId, mpsPlans.size());

        return mpsPlans;
    }

    /**
     * MPS 확정 - MpsPlan들을 기반으로 PartOrder 생성
     */
    @Transactional
    public List<PartOrderResponseDto> confirmMps(Long factoryId, Long mpsId) {
        log.info("MPS 확정 시작 - factoryId: {}, mpsId: {}", factoryId, mpsId);

        // MPS 존재 여부 확인
        Mps mps = mpsRepository.findById(mpsId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MPS_NOT_FOUND));

        // 공장 ID 검증
        if (!mps.getFactoryId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.INVALID_FACTORY_FOR_PART_ORDER);
        }

        // MpsPlan 목록 조회
        List<MpsPlan> mpsPlans = mpsPlanService.getMpsPlansByMpsId(mpsId);

        if (mpsPlans.isEmpty()) {
            throw new BadRequestException("확정할 MPS 계획이 없습니다. MPS를 먼저 실행해주세요.");
        }

        // MpsPlan별로 PartOrder 생성
        List<PartOrderResponseDto> partOrderResponses = mpsPlanService.createPartOrdersFromMpsPlans(mpsPlans);

        log.info("MPS 확정 완료 - factoryId: {}, mpsId: {}, 생성된 PartOrder 수: {}",
                factoryId, mpsId, partOrderResponses.size());

        return partOrderResponses;
    }


}
