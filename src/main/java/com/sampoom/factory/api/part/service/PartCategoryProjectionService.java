package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.dto.PartCategoryEventDto;
import com.sampoom.factory.api.part.entity.PartCategoryProjection;
import com.sampoom.factory.api.part.repository.PartCategoryProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartCategoryProjectionService {

    private final PartCategoryProjectionRepository partCategoryProjectionRepository;

    @Transactional
    public void handlePartCategoryEvent(PartCategoryEventDto eventDto) {
        final Long categoryId = eventDto.getPayload().getCategoryId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        PartCategoryProjection pcp = partCategoryProjectionRepository.findByCategoryId(categoryId).orElse(null);

        // 멱등(같은 이벤트) 차단
        if (pcp != null && eventDto.getEventId() != null && pcp.getLastEventId() != null) {
            if (pcp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순(오래된 이벤트) 차단
        if (pcp != null && incomingVer <= nvl(pcp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "PartCategoryCreated":
                handlePartCategoryCreated(eventDto);
                break;
            case "PartCategoryUpdated":
                handlePartCategoryUpdated(eventDto);
                break;
            case "PartCategoryDeleted":
                handlePartCategoryDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handlePartCategoryCreated(PartCategoryEventDto eventDto) {
        PartCategoryEventDto.PartCategoryPayload payload = eventDto.getPayload();

        PartCategoryProjection partCategoryProjection = PartCategoryProjection.builder()
                .categoryId(payload.getCategoryId())
                .categoryName(payload.getCategoryName())
                .categoryCode(payload.getCategoryCode())
                .deleted(false)
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        partCategoryProjectionRepository.save(partCategoryProjection);
        log.info("PartCategory 생성 완료: categoryId={}, name={}", payload.getCategoryId(), payload.getCategoryName());
    }

    private void handlePartCategoryUpdated(PartCategoryEventDto eventDto) {
        PartCategoryEventDto.PartCategoryPayload payload = eventDto.getPayload();

        Optional<PartCategoryProjection> existingCategory = partCategoryProjectionRepository.findByCategoryId(payload.getCategoryId());
        if (existingCategory.isEmpty()) {
            log.warn("존재하지 않는 PartCategory입니다. categoryId: {}", payload.getCategoryId());
            return;
        }

        PartCategoryProjection currentCategory = existingCategory.get();

        PartCategoryProjection updatedCategory = currentCategory.updateFromEvent(
                payload.getCategoryName(),
                payload.getCategoryCode(),
                currentCategory.getDeleted(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partCategoryProjectionRepository.save(updatedCategory);
        log.info("PartCategory 업데이트 완료: categoryId={}, name={}", payload.getCategoryId(), payload.getCategoryName());
    }

    private void handlePartCategoryDeleted(PartCategoryEventDto eventDto) {
        PartCategoryEventDto.PartCategoryPayload payload = eventDto.getPayload();

        Optional<PartCategoryProjection> existingCategory = partCategoryProjectionRepository.findByCategoryId(payload.getCategoryId());
        if (existingCategory.isEmpty()) {
            log.warn("존재하지 않는 PartCategory입니다. categoryId: {}", payload.getCategoryId());
            return;
        }

        PartCategoryProjection currentCategory = existingCategory.get();

        // Soft Delete 처리
        PartCategoryProjection deletedCategory = currentCategory.updateFromEvent(
                currentCategory.getCategoryName(),
                currentCategory.getCategoryCode(),
                true, // deleted = true
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partCategoryProjectionRepository.save(deletedCategory);
        log.info("PartCategory 삭제 완료: categoryId={}", payload.getCategoryId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}
