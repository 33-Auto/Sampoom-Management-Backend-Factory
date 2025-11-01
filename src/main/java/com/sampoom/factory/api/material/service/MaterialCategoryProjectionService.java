package com.sampoom.factory.api.material.service;

import com.sampoom.factory.api.material.dto.MaterialCategoryEventDto;
import com.sampoom.factory.api.material.entity.MaterialCategoryProjection;
import com.sampoom.factory.api.material.repository.MaterialCategoryProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialCategoryProjectionService {

    private final MaterialCategoryProjectionRepository repository;

    @Transactional
    public void handleMaterialCategoryEvent(MaterialCategoryEventDto eventDto) {
        final Long categoryId = eventDto.getPayload().getCategoryId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        MaterialCategoryProjection mcp = repository.findByCategoryId(categoryId).orElse(null);

        // 멱등성 차단
        if (mcp != null && eventDto.getEventId() != null && mcp.getLastEventId() != null) {
            if (mcp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순 차단
        if (mcp != null && incomingVer <= nvl(mcp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "MaterialCategoryCreated":
                handleCreated(eventDto);
                break;
            case "MaterialCategoryUpdated":
                handleUpdated(eventDto);
                break;
            case "MaterialCategoryDeleted":
                handleDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handleCreated(MaterialCategoryEventDto eventDto) {
        MaterialCategoryEventDto.Payload payload = eventDto.getPayload();

        MaterialCategoryProjection entity = MaterialCategoryProjection.builder()
                .categoryId(payload.getCategoryId())
                .name(payload.getName())
                .code(payload.getCode())
                .deleted(payload.getDeleted())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        repository.save(entity);
        log.info("MaterialCategory 생성 완료: categoryId={}, name={}", payload.getCategoryId(), payload.getName());
    }

    private void handleUpdated(MaterialCategoryEventDto eventDto) {
        MaterialCategoryEventDto.Payload payload = eventDto.getPayload();

        Optional<MaterialCategoryProjection> existing = repository.findByCategoryId(payload.getCategoryId());
        if (existing.isEmpty()) {
            log.warn("존재하지 않는 MaterialCategory입니다. categoryId: {}", payload.getCategoryId());
            return;
        }

        MaterialCategoryProjection current = existing.get();

        MaterialCategoryProjection updated = current.updateFromEvent(
                payload.getName(),
                payload.getCode(),
                payload.getDeleted(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        repository.save(updated);
        log.info("MaterialCategory 업데이트 완료: categoryId={}, name={}", payload.getCategoryId(), payload.getName());
    }

    private void handleDeleted(MaterialCategoryEventDto eventDto) {
        MaterialCategoryEventDto.Payload payload = eventDto.getPayload();

        Optional<MaterialCategoryProjection> existing = repository.findByCategoryId(payload.getCategoryId());
        if (existing.isEmpty()) {
            log.warn("존재하지 않는 MaterialCategory입니다. categoryId: {}", payload.getCategoryId());
            return;
        }

        MaterialCategoryProjection current = existing.get();

        MaterialCategoryProjection deleted = current.updateFromEvent(
                current.getName(),
                current.getCode(),
                true, // deleted = true
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        repository.save(deleted);
        log.info("MaterialCategory 삭제 완료: categoryId={}", payload.getCategoryId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}
