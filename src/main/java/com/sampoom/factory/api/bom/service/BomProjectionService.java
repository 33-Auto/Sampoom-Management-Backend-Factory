package com.sampoom.factory.api.bom.service;

import com.sampoom.factory.api.bom.dto.BomEventDto;
import com.sampoom.factory.api.bom.entity.BomMaterialProjection;
import com.sampoom.factory.api.bom.entity.BomProjection;
import com.sampoom.factory.api.bom.repository.BomMaterialProjectionRepository;
import com.sampoom.factory.api.bom.repository.BomProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BomProjectionService {

    private final BomProjectionRepository bomProjectionRepository;
    private final BomMaterialProjectionRepository bomMaterialProjectionRepository;

    @Transactional
    public void handleBomEvent(BomEventDto eventDto) {
        final Long bomId = eventDto.getPayload().getBomId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        BomProjection bp = bomProjectionRepository.findByBomId(bomId).orElse(null);

        // 멱등성 차단
        if (bp != null && eventDto.getEventId() != null && bp.getLastEventId() != null) {
            if (bp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순 차단
        if (bp != null && incomingVer <= nvl(bp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "BomCreated":
                handleBomCreated(eventDto);
                break;
            case "BomUpdated":
                handleBomUpdated(eventDto);
                break;
            case "BomDeleted":
                handleBomDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handleBomCreated(BomEventDto eventDto) {
        BomEventDto.Payload payload = eventDto.getPayload();

        List<BomMaterialProjection> materials = payload.getMaterials().stream()
                .map(m -> BomMaterialProjection.builder()
                        .bomId(payload.getBomId())
                        .materialId(m.getMaterialId())
                        .materialName(m.getMaterialName())
                        .materialCode(m.getMaterialCode())
                        .unit(m.getUnit())
                        .quantity(m.getQuantity())
                        .build())
                .collect(Collectors.toList());

        BomProjection bomProjection = BomProjection.builder()
                .bomId(payload.getBomId())
                .partId(payload.getPartId())
                .partCode(payload.getPartCode())
                .partName(payload.getPartName())
                .status(payload.getStatus())
                .complexity(payload.getComplexity())
                .deleted(payload.getDeleted())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        bomProjectionRepository.save(bomProjection);
        if (!materials.isEmpty()) {
            bomMaterialProjectionRepository.saveAll(materials);
        }
        log.info("BOM 생성 완료: bomId={}, partName={}", payload.getBomId(), payload.getPartName());
    }

    private void handleBomUpdated(BomEventDto eventDto) {
        BomEventDto.Payload payload = eventDto.getPayload();

        Optional<BomProjection> existingBom = bomProjectionRepository.findByBomId(payload.getBomId());
        if (existingBom.isEmpty()) {
            log.warn("존재하지 않는 BOM입니다. bomId: {}", payload.getBomId());
            return;
        }

        BomProjection currentBom = existingBom.get();

        List<BomMaterialProjection> materials = payload.getMaterials().stream()
                .map(m -> BomMaterialProjection.builder()
                        .bomId(payload.getBomId())
                        .materialId(m.getMaterialId())
                        .materialName(m.getMaterialName())
                        .materialCode(m.getMaterialCode())
                        .unit(m.getUnit())
                        .quantity(m.getQuantity())
                        .build())
                .collect(Collectors.toList());

        BomProjection updatedBom = currentBom.toBuilder()
                .partCode(payload.getPartCode())
                .partName(payload.getPartName())
                .status(payload.getStatus())
                .complexity(payload.getComplexity())
                .deleted(payload.getDeleted())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        bomProjectionRepository.save(updatedBom);
        if (!materials.isEmpty()) {
            bomMaterialProjectionRepository.saveAll(materials);
        }
        log.info("BOM 업데이트 완료: bomId={}, partName={}", payload.getBomId(), payload.getPartName());
    }

    private void handleBomDeleted(BomEventDto eventDto) {
        BomEventDto.Payload payload = eventDto.getPayload();

        Optional<BomProjection> existingBom = bomProjectionRepository.findByBomId(payload.getBomId());
        if (existingBom.isEmpty()) {
            log.warn("존재하지 않는 BOM입니다. bomId: {}", payload.getBomId());
            return;
        }

        BomProjection currentBom = existingBom.get();

        BomProjection deletedBom = currentBom.toBuilder()
                .deleted(true)
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        bomProjectionRepository.save(deletedBom);
        log.info("BOM 삭제 완료: bomId={}", payload.getBomId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}