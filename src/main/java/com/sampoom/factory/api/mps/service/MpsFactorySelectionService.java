package com.sampoom.factory.api.mps.service;

import com.sampoom.factory.api.factory.repository.BranchFactoryDistanceRepository;
import com.sampoom.factory.api.factory.entity.BranchFactoryDistance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpsFactorySelectionService {

    private final BranchFactoryDistanceRepository distanceRepository;

    /**
     * 창고에 가장 가까운 적합한 공장을 선택합니다.
     * @param warehouseId 창고 ID (branch ID로 사용)
     * @return 선택된 공장 ID
     */
    public Long selectOptimalFactory(Long warehouseId) {
        try {
            // 창고(branch)에서 가장 가까운 공장을 거리 순으로 조회
            List<BranchFactoryDistance> distances = distanceRepository.findByBranchIdOrderByDistanceKmAsc(warehouseId);

            if (distances.isEmpty()) {
                log.warn("창고 {}에 대한 공장 거리 정보가 없습니다. 기본 공장 168을 사용합니다.", warehouseId);
                return 168L; // 기본 공장 ID
            }

            // 가장 가까운 공장 선택
            BranchFactoryDistance nearestFactory = distances.get(0);
            Long selectedFactoryId = nearestFactory.getFactoryId();

            log.info("창고 {}에 대해 공장 {} 선택 (거리: {}km)",
                    warehouseId, selectedFactoryId, nearestFactory.getDistanceKm());

            return selectedFactoryId;

        } catch (Exception e) {
            log.error("공장 선택 중 오류 발생. 창고 ID: {}", warehouseId, e);
            return 168L; // 오류 시 기본 공장 ID 반환
        }
    }

    /**
     * 특정 부품을 생산할 수 있는 가장 적합한 공장을 선택합니다.
     * 현재는 거리 기반으로만 선택하지만, 향후 부품별 생산 능력 등을 고려할 수 있습니다.
     *
     * @param warehouseId 창고 ID
     * @param partId 부품 ID (향후 확장을 위해 추가)
     * @return 선택된 공장 ID
     */
    public Long selectOptimalFactoryForPart(Long warehouseId, Long partId) {
        // 현재는 거리 기반으로만 선택
        // 향후 부품별 생산 능력, 현재 작업량 등을 고려하여 확장 가능
        return selectOptimalFactory(warehouseId);
    }
}
