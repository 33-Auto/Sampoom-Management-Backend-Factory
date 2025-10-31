package com.sampoom.factory.api.material.service;

import com.sampoom.factory.api.material.dto.MaterialEventDto;
import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialProjectionService {


    private final MaterialProjectionRepository materialProjectionRepository;

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
        MaterialEventDto.Payload payload = eventDto.getPayload();

        MaterialProjection materialProjection = MaterialProjection.builder()
                .materialId(payload.getMaterialId())
                .code(payload.getMaterialCode())
                .name(payload.getName())
                .materialUnit(payload.getMaterialUnit())
                .baseQuantity(payload.getBaseQuantity())
                .leadTime(payload.getLeadTime())
                .categoryId(payload.getMaterialCategoryId())
                .deleted(payload.getDeleted())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        materialProjectionRepository.save(materialProjection);
        log.info("Material 생성 완료: materialId={}, name={}", payload.getMaterialId(), payload.getName());
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
