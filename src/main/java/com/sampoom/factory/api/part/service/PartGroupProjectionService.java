package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.dto.PartGroupEventDto;
import com.sampoom.factory.api.part.entity.PartGroupProjection;
import com.sampoom.factory.api.part.repository.PartGroupProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartGroupProjectionService {

    private final PartGroupProjectionRepository partGroupProjectionRepository;

    @Transactional
    public void handlePartGroupEvent(PartGroupEventDto eventDto) {
        final Long groupId = eventDto.getPayload().getGroupId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        PartGroupProjection pgp = partGroupProjectionRepository.findByGroupId(groupId).orElse(null);

        // 멱등(같은 이벤트) 차단
        if (pgp != null && eventDto.getEventId() != null && pgp.getLastEventId() != null) {
            if (pgp.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순(오래된 이벤트) 차단
        if (pgp != null && incomingVer <= nvl(pgp.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "PartGroupCreated":
                handlePartGroupCreated(eventDto);
                break;
            case "PartGroupUpdated":
                handlePartGroupUpdated(eventDto);
                break;
            case "PartGroupDeleted":
                handlePartGroupDeleted(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handlePartGroupCreated(PartGroupEventDto eventDto) {
        PartGroupEventDto.PartGroupPayload payload = eventDto.getPayload();

        PartGroupProjection partGroupProjection = PartGroupProjection.builder()
                .groupId(payload.getGroupId())
                .groupName(payload.getGroupName())
                .groupCode(payload.getGroupCode())
                .categoryId(payload.getCategoryId())
                .deleted(false)
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        partGroupProjectionRepository.save(partGroupProjection);
        log.info("PartGroup 생성 완료: groupId={}, name={}", payload.getGroupId(), payload.getGroupName());
    }

    private void handlePartGroupUpdated(PartGroupEventDto eventDto) {
        PartGroupEventDto.PartGroupPayload payload = eventDto.getPayload();

        Optional<PartGroupProjection> existingGroup = partGroupProjectionRepository.findByGroupId(payload.getGroupId());
        if (existingGroup.isEmpty()) {
            log.warn("존재하지 않는 PartGroup입니다. groupId: {}", payload.getGroupId());
            return;
        }

        PartGroupProjection currentGroup = existingGroup.get();

        PartGroupProjection updatedGroup = currentGroup.updateFromEvent(
                payload.getGroupName(),
                payload.getGroupCode(),
                payload.getCategoryId(),
                currentGroup.getDeleted(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partGroupProjectionRepository.save(updatedGroup);
        log.info("PartGroup 업데이트 완료: groupId={}, name={}", payload.getGroupId(), payload.getGroupName());
    }

    private void handlePartGroupDeleted(PartGroupEventDto eventDto) {
        PartGroupEventDto.PartGroupPayload payload = eventDto.getPayload();

        Optional<PartGroupProjection> existingGroup = partGroupProjectionRepository.findByGroupId(payload.getGroupId());
        if (existingGroup.isEmpty()) {
            log.warn("존재하지 않는 PartGroup입니다. groupId: {}", payload.getGroupId());
            return;
        }

        PartGroupProjection currentGroup = existingGroup.get();

        // Soft Delete 처리
        PartGroupProjection deletedGroup = currentGroup.updateFromEvent(
                currentGroup.getGroupName(),
                currentGroup.getGroupCode(),
                currentGroup.getCategoryId(),
                true, // deleted = true
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        partGroupProjectionRepository.save(deletedGroup);
        log.info("PartGroup 삭제 완료: groupId={}", payload.getGroupId());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}
