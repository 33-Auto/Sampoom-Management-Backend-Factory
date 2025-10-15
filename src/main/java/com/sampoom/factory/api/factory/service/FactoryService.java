package com.sampoom.factory.api.factory.service;

import com.sampoom.factory.api.factory.dto.FactoryCreateRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryResponseDto;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final MaterialRepository materialRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;

    @Transactional
    public FactoryResponseDto createFactory(FactoryCreateRequestDto requestDto) {
        // 공장 생성
        Factory factory = requestDto.toEntity();
        factory = factoryRepository.save(factory);

        // 모든 재료 조회
        List<Material> allMaterials = materialRepository.findAll();

        // 각 재료에 대해 공장 자재 정보 생성 (수량 0으로 설정)
        for (Material material : allMaterials) {
            FactoryMaterial factoryMaterial = FactoryMaterial.builder()
                    .factory(factory)
                    .materialId(material.getId())
                    .quantity(0L)
                    .build();

            factoryMaterialRepository.save(factoryMaterial);
        }

        return FactoryResponseDto.from(factory);
    }
}
