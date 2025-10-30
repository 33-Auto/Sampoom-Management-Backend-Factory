package com.sampoom.factory.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.material.dto.MaterialCategoryEventDto;
import com.sampoom.factory.api.material.dto.MaterialEventDto;
import com.sampoom.factory.api.material.service.MaterialCategoryProjectionService;
import com.sampoom.factory.api.material.service.MaterialProjectionService;
import com.sampoom.factory.api.part.dto.PartEventDto;
import com.sampoom.factory.api.part.dto.PartCategoryEventDto;
import com.sampoom.factory.api.part.dto.PartGroupEventDto;
import com.sampoom.factory.api.part.service.PartGroupProjectionService;
import com.sampoom.factory.api.part.service.PartProjectionService;
import com.sampoom.factory.api.part.service.PartCategoryProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectionEventHandler {

    private final ObjectMapper objectMapper;
    private final PartProjectionService partProjectionService;
    private final PartCategoryProjectionService partCategoryProjectionService;
    private final PartGroupProjectionService partGroupProjectionService;
    private final MaterialProjectionService materialProjectionService;
    private final MaterialCategoryProjectionService materialCategoryProjectionService;

    @KafkaListener(topics = "part-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePartEvent(String message) {
        handleEvent(message, "PartEvent", PartEventDto.class, partProjectionService::handlePartEvent);
    }

    @KafkaListener(topics = "part-category-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePartCategoryEvent(String message) {
        handleEvent(message, "PartCategoryEvent", PartCategoryEventDto.class, partCategoryProjectionService::handlePartCategoryEvent);
    }

    @KafkaListener(topics = "part-group-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePartGroupEvent(String message) {
        handleEvent(message, "PartGroupEvent", PartGroupEventDto.class, partGroupProjectionService::handlePartGroupEvent);
    }

    @KafkaListener(topics = "material-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleMaterialEvent(String message) {
        handleEvent(message, "MaterialEvent", MaterialEventDto.class, materialProjectionService::handleMaterialEvent);
    }

    @KafkaListener(topics = "material-category-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleMaterialCategoryEvent(String message) {
        handleEvent(message, "MaterialCategoryEvent", MaterialCategoryEventDto.class, materialCategoryProjectionService::handleMaterialCategoryEvent);
    }


    private <T> void handleEvent(String message, String eventName, Class<T> eventClass, java.util.function.Consumer<T> handler) {
        try {
            log.debug("{} 수신: {}", eventName, message);
            T event = objectMapper.readValue(message, eventClass);
            handler.accept(event);
        } catch (Exception ex) {
            log.error("{} handling failed: {}", eventName, ex.toString(), ex);
            // 컨테이너가 오프셋 커밋을 하지 않도록 런타임 예외로 던져 재시도/ DLQ 흐름
            throw new RuntimeException("Kafka handling failed", ex);
        }
    }
}