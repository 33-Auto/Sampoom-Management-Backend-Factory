package com.sampoom.factory.api.mps.service;

import com.sampoom.factory.api.mps.dto.PartForecastEvent;
import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsStatus;
import com.sampoom.factory.api.mps.repository.MpsRepository;
import com.sampoom.factory.api.part.entity.PartProjection;
import com.sampoom.factory.api.part.repository.PartProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpsEventService {

    private final MpsRepository mpsRepository;
    private final PartProjectionRepository partProjectionRepository;
    private final MpsFactorySelectionService factorySelectionService;

    public void processPartForecastEvent(PartForecastEvent event) {
        PartForecastEvent.PartForecastPayload payload = event.getPayload();

        // PartProjection에서 standardQuantity, leadTime, baseQuantity 정보 조회
        PartProjection partProjection = partProjectionRepository.findByPartId(payload.getPartId())
                .orElseThrow(() -> new IllegalArgumentException("부품 정보를 찾을 수 없습니다: " + payload.getPartId()));

        // 적합한 공장 자동 선택
        Long selectedFactoryId = factorySelectionService.selectOptimalFactoryForPart(
                payload.getWarehouseId(), payload.getPartId());

        // 안전 재고는 part의 base_quantity 사용
        Integer safetyStock = partProjection.getBaseQuantity();

        // 목표 날짜 계산 - 수요 월의 전 월 마지막 날로 설정
        LocalDate demandDate = payload.getDemandMonth().toLocalDate();
        LocalDate targetDate = demandDate.minusMonths(1).withDayOfMonth(
                demandDate.minusMonths(1).lengthOfMonth()
        );

        // 여유 시간 기본값 (7일)
        Integer bufferDays = 7;

        log.info("MPS 자동 생성 - partId: {}, warehouseId: {}, factoryId: {}, 예측수량: {}, 예상재고: {}, 안전재고(baseQuantity): {}, 수요월: {}, 목표날짜: {}",
                payload.getPartId(), payload.getWarehouseId(), selectedFactoryId, payload.getDemandQuantity(),
                payload.getStock(), safetyStock, demandDate.getMonth(), targetDate);

        // MPS 자동 생성 (warehouseId, factoryId 포함)
        Mps mps = Mps.builder()
                .partId(payload.getPartId())
                .warehouseId(payload.getWarehouseId())
                .factoryId(selectedFactoryId)
                .standardQuantity(partProjection.getStandardQuantity())
                .expectedInventory(payload.getStock())
                .forecastQuantity(payload.getDemandQuantity())
                .safetyStock(safetyStock)
                .leadTime(partProjection.getLeadTime())
                .targetDate(targetDate)
                .bufferDays(bufferDays)
                .build();

        // MPS 계산 로직 적용
        int requiredQuantity = mps.getForecastQuantity() + mps.getSafetyStock() - mps.getExpectedInventory();
        int productionCycles = requiredQuantity > 0 ?
            (int) Math.ceil((double) requiredQuantity / mps.getStandardQuantity()) : 0;
        int totalProduction = Math.max(requiredQuantity, 0);
        int requiredDays = productionCycles * mps.getLeadTime();
        LocalDate startDate = mps.getTargetDate().minusDays(requiredDays + mps.getBufferDays());

        // 계산된 값으로 MPS 업데이트
        mps = mps.toBuilder()
                .productionCycles(productionCycles)
                .totalProduction(totalProduction)
                .startDate(startDate)
                .status(MpsStatus.PLANNED)
                .build();

        Mps savedMps = mpsRepository.save(mps);

        log.info("MPS 자동 생성 완료 - mpsId: {}, 시작일: {}, 총생산량: {}",
                savedMps.getMpsId(), savedMps.getStartDate(), savedMps.getTotalProduction());
    }
}
