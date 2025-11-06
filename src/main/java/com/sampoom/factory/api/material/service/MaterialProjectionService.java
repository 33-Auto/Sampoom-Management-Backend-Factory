package com.sampoom.factory.api.material.service;

import com.sampoom.factory.api.material.dto.MaterialEventDto;
import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.factory.entity.FactoryProjection;
import com.sampoom.factory.api.factory.repository.FactoryProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialProjectionService {

    private final MaterialProjectionRepository materialProjectionRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final FactoryProjectionRepository factoryProjectionRepository;

    @Transactional
    public void handleMaterialEvent(MaterialEventDto eventDto) {
        final Long materialId = eventDto.getPayload().getMaterialId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        MaterialProjection mp = materialProjectionRepository.findByMaterialId(materialId).orElse(null);

        // 멱등(같은 이벤트) 차단
        if (mp != null && eventDto.getEventId() != null && mp.getLastEventId() != null) {
            if (mp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순(오래된 이벤트) 차단
        if (mp != null && incomingVer <= nvl(mp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "MaterialCreated":
                handleMaterialCreated(eventDto);
                break;
            case "MaterialUpdated":
                handleMaterialUpdated(eventDto);
                break;
            case "MaterialDeleted":
                handleMaterialDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handleMaterialCreated(MaterialEventDto eventDto) {
        log.info("Material 생성 이벤트 처리: materialId={}", eventDto.getPayload().getMaterialId());

        MaterialProjection mp = MaterialProjection.builder()
                .materialId(eventDto.getPayload().getMaterialId())
                .code(eventDto.getPayload().getMaterialCode())
                .name(eventDto.getPayload().getName())
                .baseQuantity(eventDto.getPayload().getBaseQuantity())
                .categoryId(eventDto.getPayload().getMaterialCategoryId())
                .standardCost(eventDto.getPayload().getStandardCost())
                .materialUnit(eventDto.getPayload().getMaterialUnit())
                .leadTime(eventDto.getPayload().getLeadTime())
                .version(eventDto.getVersion())
                .lastEventId(eventDto.getEventId())
                .deleted(false)
                .build();

        materialProjectionRepository.save(mp);
        log.info("MaterialProjection 저장 완료: materialId={}", mp.getMaterialId());

        // 모든 기존 공장에 새로운 자재를 수량 0으로 자동 연결
        initializeMaterialToAllFactories(eventDto.getPayload().getMaterialId());
    }

    /**
     * 새로 생성된 자재를 모든 기존 공장에 수량 0으로 초기화
     */
    private void initializeMaterialToAllFactories(Long materialId) {
        log.info("자재 공장 연결 초기화 시작: materialId={}", materialId);

        // 모든 활성 공장 조회
        List<FactoryProjection> allFactories = factoryProjectionRepository.findAll();

        if (allFactories.isEmpty()) {
            log.info("연결할 공장이 없습니다: materialId={}", materialId);
            return;
        }

        // 각 공장에 새 자재를 수량 0으로 연결
        List<FactoryMaterial> factoryMaterials = allFactories.stream()
                .filter(factory -> !factory.getDeleted()) // 삭제되지 않은 공장만
                .map(factory -> FactoryMaterial.builder()
                        .factoryId(factory.getBranchId())
                        .materialId(materialId)
                        .quantity(0L)
                        .build())
                .toList();

        if (!factoryMaterials.isEmpty()) {
            factoryMaterialRepository.saveAll(factoryMaterials);
            log.info("자재 공장 연결 초기화 완료: materialId={}, 연결된 공장 수={}",
                    materialId, factoryMaterials.size());
        }
    }

    private void handleMaterialUpdated(MaterialEventDto eventDto) {
        MaterialEventDto.Payload payload = eventDto.getPayload();

        Optional<MaterialProjection> existingMaterial = materialProjectionRepository.findByMaterialId(payload.getMaterialId());
        if (existingMaterial.isEmpty()) {
            log.warn("존재하지 않는 Material materialId: {}", payload.getMaterialId());
            return;
        }

        MaterialProjection currentMaterial = existingMaterial.get();

        MaterialProjection updatedMaterial = currentMaterial.updateFromEvent(
                payload.getMaterialCode(),
                payload.getName(),
                payload.getMaterialUnit(),
                payload.getBaseQuantity(),
                payload.getLeadTime(),
                payload.getStandardCost(),
                payload.getDeleted(),
                payload.getMaterialCategoryId(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        materialProjectionRepository.save(updatedMaterial);
        log.info("Material 업데이트 완료: materialId={}, name={}", payload.getMaterialId(), payload.getName());
    }

    private void handleMaterialDeleted(MaterialEventDto eventDto) {
        MaterialEventDto.Payload payload = eventDto.getPayload();

        Optional<MaterialProjection> existingMaterial = materialProjectionRepository.findByMaterialId(payload.getMaterialId());
        if (existingMaterial.isEmpty()) {
            log.warn("존재하지 않는 Material. materialId: {}", payload.getMaterialId());
            return;
        }

        MaterialProjection currentMaterial = existingMaterial.get();

        // Soft Delete 처리
        MaterialProjection deletedMaterial = currentMaterial.updateFromEvent(
                currentMaterial.getCode(),
                currentMaterial.getName(),
                currentMaterial.getMaterialUnit(),
                currentMaterial.getBaseQuantity(),
                currentMaterial.getLeadTime(),
                currentMaterial.getStandardCost(),
                true, // deleted = true
                currentMaterial.getCategoryId(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        materialProjectionRepository.save(deletedMaterial);
        log.info("Material 삭제 완료: materialId={}", payload.getMaterialId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }

}
