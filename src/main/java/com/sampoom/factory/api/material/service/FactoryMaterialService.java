package com.sampoom.factory.api.material.service;

import com.sampoom.factory.api.material.dto.MaterialResponseDto;
import com.sampoom.factory.common.response.PageResponseDto;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.factory.repository.FactoryRepository;

import com.sampoom.factory.api.material.repository.MaterialCategoryProjectionRepository;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactoryMaterialService {

    private final FactoryRepository factoryRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final MaterialCategoryProjectionRepository materialCategoryProjectionRepository;
    private final MaterialProjectionRepository materialProjectionRepository;


    public PageResponseDto<MaterialResponseDto> getMaterialsByFactoryAndCategory(
            Long factoryId, Long categoryId, int page, int size) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        materialCategoryProjectionRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CATEGORY_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<FactoryMaterial> materialsPage = factoryMaterialRepository
                .findByFactoryAndCategory(factoryId, categoryId, pageable);

        List<MaterialResponseDto> content = materialsPage.getContent().stream()
                .map(factoryMaterial -> {
                    Long materialId = factoryMaterial.getMaterialId();
                    MaterialProjection materialProjection = materialProjectionRepository.findByMaterialId(materialId)
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                    MaterialResponseDto dto = MaterialResponseDto.from(materialProjection)
                            .withQuantity(factoryMaterial.getQuantity());
                    if (materialProjection.getCategoryId() != null) {
                        materialCategoryProjectionRepository.findByCategoryId(materialProjection.getCategoryId())
                                .ifPresent(category -> dto.withCategoryName(category.getName()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponseDto.<MaterialResponseDto>builder()
                .content(content)
                .totalElements(materialsPage.getTotalElements())
                .totalPages(materialsPage.getTotalPages())
                .build();
    }


    @Transactional(readOnly = true)
    public PageResponseDto<MaterialResponseDto> searchMaterials(
            Long factoryId,
            Long categoryId,
            String keyword,
            int page,
            int size
    ) {

        factoryRepository.findById(factoryId)
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        if (categoryId != null) {
            materialCategoryProjectionRepository.findByCategoryId(categoryId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.CATEGORY_NOT_FOUND));
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<FactoryMaterial> fmPage;
        if (keyword == null || keyword.isBlank()) {
            fmPage = factoryMaterialRepository.findByFactoryAndCategory(
                    factoryId, categoryId, pageable);
        } else {
            fmPage = factoryMaterialRepository.findByFactoryCategoryAndKeyword(
                    factoryId, categoryId, keyword, pageable);
        }
        List<MaterialResponseDto> content = fmPage.getContent().stream()
                .map(factoryMaterial -> {
                    Long materialId = factoryMaterial.getMaterialId();
                    MaterialProjection materialProjection = materialProjectionRepository.findByMaterialId(materialId)
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                    MaterialResponseDto dto = MaterialResponseDto.from(materialProjection)
                            .withQuantity(factoryMaterial.getQuantity());
                    if (materialProjection.getCategoryId() != null) {
                        materialCategoryProjectionRepository.findByCategoryId(materialProjection.getCategoryId())
                                .ifPresent(category -> dto.withCategoryName(category.getName()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponseDto.<MaterialResponseDto>builder()
                .content(content)
                .totalElements(fmPage.getTotalElements())
                .totalPages(fmPage.getTotalPages())
                .build();
    }


}