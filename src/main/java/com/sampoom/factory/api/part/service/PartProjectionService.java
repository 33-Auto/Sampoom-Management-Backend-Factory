package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.dto.PartEventDto;
import com.sampoom.factory.api.part.entity.PartProjection;
import com.sampoom.factory.api.part.entity.PartStatus;
import com.sampoom.factory.api.part.repository.PartProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartProjectionService {

    private final PartProjectionRepository partProjectionRepository;

    @Transactional
    public void handlePartEvent(PartEventDto eventDto) {
        final Long partId = eventDto.getPayload().getPartId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        PartProjection pp = partProjectionRepository.findByPartId(partId).orElse(null);

        // 멱등(같은 이벤트) 차단
        if (pp != null && eventDto.getEventId() != null && pp.getLastEventId() != null) {
            if (pp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순(오래된 이벤트) 차단
        if (pp != null && incomingVer <= nvl(pp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "PartCreated":
                handlePartCreated(eventDto);
                break;
            case "PartUpdated":
                handlePartUpdated(eventDto);
                break;
            case "PartDeleted":
                handlePartDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handlePartCreated(PartEventDto eventDto) {
        PartEventDto.PartPayload payload = eventDto.getPayload();

        PartProjection partProjection = PartProjection.builder()
                .partId(payload.getPartId())
                .code(payload.getCode())
                .name(payload.getName())
                .partUnit(payload.getPartUnit())
                .baseQuantity(payload.getBaseQuantity())
                .leadTime(payload.getLeadTime())
                .status(PartStatus.valueOf(payload.getStatus()))
                .deleted(payload.getDeleted())
                .groupId(payload.getGroupId())
                .categoryId(payload.getCategoryId())
                .standardCost(payload.getStandardCost())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        partProjectionRepository.save(partProjection);
        log.info("Part 생성 완료: partId={}, name={}", payload.getPartId(), payload.getName());
    }

    private void handlePartUpdated(PartEventDto eventDto) {
        PartEventDto.PartPayload payload = eventDto.getPayload();

        Optional<PartProjection> existingPart = partProjectionRepository.findByPartId(payload.getPartId());
        if (existingPart.isEmpty()) {
            log.warn("존재하지 않는 Part입니다. partId: {}", payload.getPartId());
            return;
        }

        PartProjection currentPart = existingPart.get();

        PartProjection updatedPart = currentPart.updateFromEvent(
                payload.getCode(),
                payload.getName(),
                payload.getPartUnit(),
                payload.getBaseQuantity(),
                payload.getLeadTime(),
                PartStatus.valueOf(payload.getStatus()),
                payload.getDeleted(),
                payload.getGroupId(),
                payload.getCategoryId(),
                payload.getStandardCost(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partProjectionRepository.save(updatedPart);
        log.info("Part 업데이트 완료: partId={}, name={}", payload.getPartId(), payload.getName());
    }

    private void handlePartDeleted(PartEventDto eventDto) {
        PartEventDto.PartPayload payload = eventDto.getPayload();

        Optional<PartProjection> existingPart = partProjectionRepository.findByPartId(payload.getPartId());
        if (existingPart.isEmpty()) {
            log.warn("존재하지 않는 Part입니다. partId: {}", payload.getPartId());
            return;
        }

        PartProjection currentPart = existingPart.get();

        // Soft Delete 처리
        PartProjection deletedPart = currentPart.updateFromEvent(
                currentPart.getCode(),
                currentPart.getName(),
                currentPart.getPartUnit(),
                currentPart.getBaseQuantity(),
                currentPart.getLeadTime(),
                currentPart.getStatus(),
                true, // deleted = true
                currentPart.getGroupId(),
                currentPart.getCategoryId(),
                currentPart.getStandardCost(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partProjectionRepository.save(deletedPart);
        log.info("Part 삭제 완료: partId={}", payload.getPartId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}
