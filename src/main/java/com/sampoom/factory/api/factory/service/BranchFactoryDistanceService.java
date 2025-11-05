package com.sampoom.factory.api.factory.service;

import com.sampoom.factory.api.factory.dto.BranchFactoryDistanceEventDto;
import com.sampoom.factory.api.factory.entity.BranchFactoryDistance;
import com.sampoom.factory.api.factory.repository.BranchFactoryDistanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchFactoryDistanceService {

    private final BranchFactoryDistanceRepository distanceRepository;

    @Transactional
    public void handleDistanceEvent(BranchFactoryDistanceEventDto eventDto) {
        final Long distanceId = eventDto.getPayload().getDistanceId();
        final Long incomingVer = nvl(eventDto.getVersion(), 0L);

        BranchFactoryDistance existingDistance = distanceRepository.findByDistanceId(distanceId).orElse(null);

        // 멱등(같은 이벤트) 차단
        if (existingDistance != null && eventDto.getEventId() != null && existingDistance.getLastEventId() != null) {
            if (existingDistance.getLastEventId().equals(eventDto.getEventId())) return;
        }
        // 역순(오래된 이벤트) 차단
        if (existingDistance != null && incomingVer <= nvl(existingDistance.getVersion(), 0L)) return;

        switch (eventDto.getEventType()) {
            case "BranchFactoryDistanceCalculated":
                handleDistanceCalculated(eventDto);
                break;
            case "BranchFactoryDistanceUpdated":
                handleDistanceUpdated(eventDto);
                break;
            default:
                log.warn("알 수 없는 이벤트 타입: {}", eventDto.getEventType());
        }
    }

    private void handleDistanceCalculated(BranchFactoryDistanceEventDto eventDto) {
        BranchFactoryDistanceEventDto.Payload payload = eventDto.getPayload();

        BranchFactoryDistance distance = BranchFactoryDistance.builder()
                .distanceId(payload.getDistanceId())
                .branchId(payload.getBranchId())
                .factoryId(payload.getFactoryId())
                .distanceKm(payload.getDistanceKm())
                .branchName(payload.getBranchName())
                .factoryName(payload.getFactoryName())
                .lastEventId(eventDto.getEventId())
                .version(eventDto.getVersion())
                .sourceUpdatedAt(eventDto.getOccurredAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        distanceRepository.save(distance);
        log.info("창고-공장 거리 계산 완료: branchName={}, factoryName={}, distance={}km",
                payload.getBranchName(), payload.getFactoryName(), payload.getDistanceKm());
    }

    private void handleDistanceUpdated(BranchFactoryDistanceEventDto eventDto) {
        BranchFactoryDistanceEventDto.Payload payload = eventDto.getPayload();

        Optional<BranchFactoryDistance> existingDistance = distanceRepository.findByDistanceId(payload.getDistanceId());
        if (existingDistance.isEmpty()) {
            log.warn("존재하지 않는 거리 정보입니다. distanceId: {}", payload.getDistanceId());
            return;
        }

        BranchFactoryDistance currentDistance = existingDistance.get();

        BranchFactoryDistance updatedDistance = currentDistance.updateFromEvent(
                payload.getBranchId(),
                payload.getFactoryId(),
                payload.getDistanceKm(),
                payload.getBranchName(),
                payload.getFactoryName(),
                eventDto.getEventId(),
                eventDto.getVersion(),
                eventDto.getOccurredAt()
        );

        distanceRepository.save(updatedDistance);
        log.info("창고-공장 거리 업데이트 완료: branchName={}, factoryName={}, distance={}km",
                payload.getBranchName(), payload.getFactoryName(), payload.getDistanceKm());
    }

    private long nvl(Long v, long def) { return v == null ? def : v; }
}
