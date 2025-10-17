package com.sampoom.factory.api.bom.service;

import com.sampoom.factory.api.bom.dto.BomDetailResponseDto;
import com.sampoom.factory.api.bom.dto.BomRequestDto;
import com.sampoom.factory.api.bom.dto.BomResponseDto;
import com.sampoom.factory.api.bom.entity.Bom;
import com.sampoom.factory.api.bom.entity.BomMaterial;
import com.sampoom.factory.api.bom.repository.BomRepository;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.repository.MaterialRepository;
import com.sampoom.factory.api.part.entity.Part;
import com.sampoom.factory.api.part.repository.PartRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import com.sampoom.factory.common.response.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BomService {
    private final BomRepository bomRepository;
    private final PartRepository partRepository;
    private final MaterialRepository materialRepository;


    @Transactional
    public BomResponseDto createBom(BomRequestDto requestDto) {
        Part part = partRepository.findById(requestDto.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));

        Bom bom = Bom.builder()
                .part(part)
                .materials(new ArrayList<>())
                .build();

        for (BomRequestDto.BomMaterialDto materialDto : requestDto.getMaterials()) {
            Material material = materialRepository.findById(materialDto.getMaterialId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));

            BomMaterial bomMaterial = BomMaterial.builder()
                    .bom(bom)
                    .material(material)
                    .quantity(materialDto.getQuantity())
                    .build();

            bom.addMaterial(bomMaterial);
        }

        return BomResponseDto.from(bomRepository.save(bom));
    }


    public PageResponseDto<BomResponseDto> getBoms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bom> bomPage = bomRepository.findAll(pageable);

        return PageResponseDto.<BomResponseDto>builder()
                .content(bomPage.getContent().stream()
                        .map(BomResponseDto::from)
                        .collect(Collectors.toList()))
                .totalPages(bomPage.getTotalPages())
                .totalElements(bomPage.getTotalElements())
                .build();
    }


    public BomDetailResponseDto getBomDetail(Long bomId) {
        Bom bom = bomRepository.findById(bomId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

        return BomDetailResponseDto.from(bom);
    }


    @Transactional
    public BomResponseDto updateBom(Long bomId, BomRequestDto requestDto) {
        Bom bom = bomRepository.findById(bomId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

        // 기존 자재 삭제
        bom.getMaterials().clear();

        // 새 자재 추가
        for (BomRequestDto.BomMaterialDto materialDto : requestDto.getMaterials()) {
            Material material = materialRepository.findById(materialDto.getMaterialId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));

            BomMaterial bomMaterial = BomMaterial.builder()
                    .bom(bom)
                    .material(material)
                    .quantity(materialDto.getQuantity())
                    .build();

            bom.addMaterial(bomMaterial);
        }
        bom.touchNow();

        return BomResponseDto.from(bom);
    }


    @Transactional
    public void deleteBom(Long bomId) {
        Bom bom = bomRepository.findById(bomId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

        bomRepository.delete(bom);
    }


    public PageResponseDto<BomResponseDto> searchBoms(String keyword, Long categoryId, Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bom> bomPage = bomRepository.findByFilters(keyword, categoryId, groupId, pageable);

        return PageResponseDto.<BomResponseDto>builder()
                .content(bomPage.getContent().stream()
                        .map(BomResponseDto::from)
                        .collect(Collectors.toList()))
                .totalPages(bomPage.getTotalPages())
                .totalElements(bomPage.getTotalElements())
                .build();
    }
}
