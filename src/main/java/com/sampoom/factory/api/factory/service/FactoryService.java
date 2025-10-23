package com.sampoom.factory.api.factory.service;


import com.sampoom.factory.api.factory.dto.FactoryRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryResponseDto;
import com.sampoom.factory.api.factory.entity.Factory;

import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final MaterialRepository materialRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final FactoryEventService factoryEventService;

    @Transactional
    public FactoryResponseDto createFactory(FactoryRequestDto requestDto) {
        // 공장 생성
        Factory factory = requestDto.toEntity();
        factory = factoryRepository.saveAndFlush(factory);

        // 모든 재료 조회
        List<Material> allMaterials = materialRepository.findAll();

        // 각 재료에 대해 공장 자재 정보 생성 (수량 0으로 설정)
        for (Material material : allMaterials) {
            FactoryMaterial factoryMaterial = FactoryMaterial.builder()
                    .factory(factory)
                    .material(material)
                    .quantity(0L)
                    .build();

            factoryMaterialRepository.save(factoryMaterial);
        }

        factoryEventService.recordFactoryCreated(factory);

        return FactoryResponseDto.from(factory);
    }

    @Transactional
    public FactoryResponseDto updateFactory(Long factoryId, FactoryRequestDto requestDto) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        factory.update(requestDto);

        factoryRepository.flush();

        factoryEventService.recordFactoryUpdated(factory);

        return FactoryResponseDto.from(factory);
    }

    @Transactional
    public void deleteFactory(Long factoryId) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        // 관련 데이터 삭제 처리
        factoryMaterialRepository.deleteAllByFactory(factory);
        factoryRepository.delete(factory);
        factoryEventService.recordFactoryDeleted(factory);
    }

    @Transactional(readOnly = true)
    public FactoryResponseDto getFactory(Long factoryId) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));


        return FactoryResponseDto.from(factory);
    }

    @Transactional(readOnly = true)
    public List<FactoryResponseDto> getAllFactories() {
        return factoryRepository.findAll().stream()
                .map(FactoryResponseDto::from)
                .collect(Collectors.toList());
    }
}
