package com.sampoom.factory.api.factory.service;

import com.sampoom.factory.api.factory.dto.BranchEventDto;
import com.sampoom.factory.api.factory.entity.FactoryProjection;
import com.sampoom.factory.api.factory.entity.FactoryStatus;
import com.sampoom.factory.api.factory.repository.FactoryProjectionRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchProjectionService {

    private final FactoryProjectionRepository factoryProjectionRepository;
    private final MaterialProjectionRepository materialProjectionRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;

    @Transactional
    public void handleBranchEvent(BranchEventDto eventDto) {
        switch (eventDto.getEventType()) {
            case "BranchCreated":
                handleBranchCreated(eventDto);
                break;
            case "BranchUpdated":
                handleBranchUpdated(eventDto);
                break;
            case "BranchDeleted":
                handleBranchDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handleBranchCreated(BranchEventDto eventDto) {
        log.info("Branch 생성 이벤트 처리: branchId={}", eventDto.getPayload().getBranchId());

        FactoryProjection projection = FactoryProjection.builder()
                .branchId(eventDto.getPayload().getBranchId())
                .branchCode(eventDto.getPayload().getBranchCode())
                .branchName(eventDto.getPayload().getBranchName())
                .address(eventDto.getPayload().getAddress())
                .latitude(eventDto.getPayload().getLatitude())
                .longitude(eventDto.getPayload().getLongitude())
                .status(FactoryStatus.valueOf(eventDto.getPayload().getStatus()))
                .deleted(eventDto.getPayload().getDeleted())
                .build();

        factoryProjectionRepository.save(projection);
        log.info("FactoryProjection 저장 완료: branchId={}", projection.getBranchId());

        // 모든 자재를 해당 공장에 수량 0으로 자동 연결
        initializeFactoryMaterials(eventDto.getPayload().getBranchId());
    }

    /**
     * 새로 생성된 공장에 모든 자재를 수량 0으로 초기화
     */
    private void initializeFactoryMaterials(Long factoryId) {
        log.info("공장 자재 초기화 시작: factoryId={}", factoryId);

        // 모든 자재 조회
        List<MaterialProjection> allMaterials = materialProjectionRepository.findAll();

        if (allMaterials.isEmpty()) {
            log.info("초기화할 자재가 없습니다: factoryId={}", factoryId);
            return;
        }

        // 각 자재를 공장에 수량 0으로 연결
        List<FactoryMaterial> factoryMaterials = allMaterials.stream()
                .map(material -> FactoryMaterial.builder()
                        .factoryId(factoryId)
                        .materialId(material.getMaterialId())
                        .quantity(0L)
                        .build())
                .toList();

        factoryMaterialRepository.saveAll(factoryMaterials);

        log.info("공장 자재 초기화 완료: factoryId={}, 연결된 자재 수={}",
                factoryId, factoryMaterials.size());
    }

    private void handleBranchUpdated(BranchEventDto eventDto) {
        log.info("Branch 수정 이벤트 처리: branchId={}", eventDto.getPayload().getBranchId());

        FactoryProjection projection = factoryProjectionRepository.findById(eventDto.getPayload().getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("FactoryProjection을 찾을 수 없습니다: " + eventDto.getPayload().getBranchId()));

        projection.updateFromEvent(
                eventDto.getPayload().getBranchCode(),
                eventDto.getPayload().getBranchName(),
                eventDto.getPayload().getAddress(),
                eventDto.getPayload().getLatitude(),
                eventDto.getPayload().getLongitude(),
                FactoryStatus.valueOf(eventDto.getPayload().getStatus()),
                eventDto.getPayload().getDeleted()
        );

        factoryProjectionRepository.save(projection);
        log.info("FactoryProjection 수정 완료: branchId={}", projection.getBranchId());
    }

    private void handleBranchDeleted(BranchEventDto eventDto) {
        log.info("Branch 삭제 이벤트 처리: branchId={}", eventDto.getPayload().getBranchId());

        FactoryProjection projection = factoryProjectionRepository.findById(eventDto.getPayload().getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("FactoryProjection을 찾을 수 없습니다: " + eventDto.getPayload().getBranchId()));

        projection.updateFromEvent(
                projection.getBranchCode(),
                projection.getBranchName(),
                projection.getAddress(),
                projection.getLatitude(),
                projection.getLongitude(),
                projection.getStatus(),
                true // deleted = true
        );

        factoryProjectionRepository.save(projection);
        log.info("FactoryProjection 삭제 마킹 완료: branchId={}", projection.getBranchId());
    }
}
