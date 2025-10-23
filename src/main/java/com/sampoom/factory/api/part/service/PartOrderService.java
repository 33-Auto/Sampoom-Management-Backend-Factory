package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.bom.entity.Bom;
import com.sampoom.factory.api.bom.entity.BomMaterial;
import com.sampoom.factory.api.bom.repository.BomRepository;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.dto.MaterialOrderResponseDto;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.Part;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderItem;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import com.sampoom.factory.api.part.repository.PartRepository;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import com.sampoom.factory.common.response.PageResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartOrderService {
    private final BomRepository bomRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final PartRepository partRepository;
    private final PartOrderRepository partOrderRepository;
    private final FactoryRepository factoryRepository;


    @Transactional
    public PartOrderResponseDto completePartOrder(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (partOrder.getStatus() != PartOrderStatus.IN_PRODUCTION) {
            throw new BadRequestException(ErrorStatus.ORDER_NOT_IN_PRODUCTION);
        }


        partOrder.updateStatus(PartOrderStatus.COMPLETED);



        List<PartOrderResponseDto.PartOrderItemDto> itemDtos = partOrder.getItems().stream()
                .map(item -> PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(item.getPart().getId())
                        .partName(item.getPart().getName())
                        .partCode(item.getPart().getCode())
                        .partGroup(item.getPart().getGroup().getName())
                        .partCategory(item.getPart().getGroup().getCategory().getName())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(partOrder.getStatus().name())
                .items(itemDtos)
                .factoryId(partOrder.getFactory().getId())
                .factoryName(partOrder.getFactory().getName())
                .build();
    }

    public PartOrderResponseDto getPartOrder(Long factoryId, Long orderId) {
        // 주문 ID로 주문 조회
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // 요청한 공장의 주문인지 검증
        if (!partOrder.getFactory().getId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.INVALID_FACTORY_FOR_PART_ORDER);
        }

        List<PartOrderResponseDto.PartOrderItemDto> itemDtos = partOrder.getItems().stream()
                .map(item -> PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(item.getPart().getId())
                        .partName(item.getPart().getName())
                        .partCode(item.getPart().getCode())
                        .partGroup(item.getPart().getGroup().getName())
                        .partCategory(item.getPart().getGroup().getCategory().getName())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(partOrder.getStatus().name())
                .items(itemDtos)
                .factoryId(partOrder.getFactory().getId())
                .factoryName(partOrder.getFactory().getName())
                .build();
    }

    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, PartOrderStatus status, int page, int size) {
        factoryRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));


        Pageable pageable = PageRequest.of(page, size);


        Page<PartOrder> partOrderPage;
        if (status != null) {
            partOrderPage = partOrderRepository.findByFactoryIdAndStatus(factoryId, status, pageable);
        } else {
            partOrderPage = partOrderRepository.findByFactoryId(factoryId, pageable);
        }


        List<PartOrderResponseDto> content = partOrderPage.getContent().stream()
                .map(partOrder -> {
                    List<PartOrderResponseDto.PartOrderItemDto> itemDtos = partOrder.getItems().stream()
                            .map(item -> PartOrderResponseDto.PartOrderItemDto.builder()
                                    .partId(item.getPart().getId())
                                    .partName(item.getPart().getName())
                                    .partCode(item.getPart().getCode())
                                    .partGroup(item.getPart().getGroup().getName())
                                    .partCategory(item.getPart().getGroup().getCategory().getName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList();

                    return PartOrderResponseDto.builder()
                            .orderId(partOrder.getId())
                            .warehouseName(partOrder.getWarehouseName())
                            .orderDate(partOrder.getOrderDate())
                            .status(partOrder.getStatus().name())
                            .items(itemDtos)
                            .factoryId(partOrder.getFactory().getId())
                            .factoryName(partOrder.getFactory().getName())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    @Transactional
    public PartOrderResponseDto cancelPartOrder(Long factoryId, Long orderId) {
        // 주문 조회
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // 요청한 공장의 주문인지 확인
        if (!partOrder.getFactory().getId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.INVALID_FACTORY_FOR_PART_ORDER);
        }

        // 취소 가능한 상태인지 확인
        if (partOrder.getStatus() != PartOrderStatus.REQUESTED) {
            throw new BadRequestException(ErrorStatus.CANNOT_CANCEL_PROCESSED_ORDER);
        }

        // 주문 상태 변경
        partOrder.updateStatus(PartOrderStatus.CANCELED);
        partOrderRepository.save(partOrder);

        List<PartOrderResponseDto.PartOrderItemDto> itemDtos = partOrder.getItems().stream()
                .map(item -> PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(item.getPart().getId())
                        .partName(item.getPart().getName())
                        .partCode(item.getPart().getCode())
                        .partGroup(item.getPart().getGroup().getName())
                        .partCategory(item.getPart().getGroup().getCategory().getName())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(partOrder.getStatus().name())
                .items(itemDtos)
                .factoryId(partOrder.getFactory().getId())
                .factoryName(partOrder.getFactory().getName())
                .build();
    }

    @Transactional
    public PartOrderResponseDto createPartOrder(PartOrderRequestDto request) {
        // 주문에 필요한 부품 및 자재 식별
        Map<Long, Long> requiredMaterials = calculateRequiredMaterials(request);

        // 적합한 공장 선택
        Factory factory = findOptimalFactory(requiredMaterials, request.getWarehouseName());


        PartOrder partOrder = PartOrder.builder()
                .factory(factory)
                .status(PartOrderStatus.REQUESTED)
                .warehouseName(request.getWarehouseName())
                .orderDate(LocalDateTime.now())
                .build();

        List<PartOrderItem> items = new ArrayList<>();
        boolean lackOfMaterial = false;
        Map<Long, Bom> bomCache = new HashMap<>();


        for (PartOrderRequestDto.PartOrderItemRequestDto itemReq : request.getItems()) {
            Part part = partRepository.findById(itemReq.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));
            Bom bom = bomRepository.findByPart_Id(itemReq.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            bomCache.put(itemReq.getPartId(), bom);

            // 자재 차감 가능 여부 확인
            for (BomMaterial bomMaterial : bom.getMaterials()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(factory.getId(), bomMaterial.getMaterial().getId())
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                long required = bomMaterial.getQuantity() * itemReq.getQuantity();
                if (factoryMaterial.getQuantity() < required) {
                    lackOfMaterial = true;
                }
            }

            items.add(PartOrderItem.builder()
                    .partOrder(partOrder)
                    .part(part)
                    .quantity(itemReq.getQuantity())
                    .build());
        }

        partOrder.getItems().addAll(items);

        if (lackOfMaterial) {
            partOrder.updateStatus(PartOrderStatus.LACK_OF_MATERIAL);
        } else {
            partOrder.updateStatus(PartOrderStatus.IN_PRODUCTION);
            // 자재 차감 로직 추가
            for (PartOrderItem item : items) {
                Bom bom = bomCache.get(item.getPart().getId());
                for (BomMaterial bomMaterial : bom.getMaterials()) {
                    FactoryMaterial factoryMaterial = factoryMaterialRepository
                            .findByFactoryIdAndMaterialId(factory.getId(), bomMaterial.getMaterial().getId())
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                    long required = bomMaterial.getQuantity() * item.getQuantity();
                    factoryMaterial.decreaseQuantity(required);
                }
            }
        }

        partOrderRepository.save(partOrder);

        // DTO 변환
        List<PartOrderResponseDto.PartOrderItemDto> itemDtos = items.stream()
                .map(item -> PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(item.getPart().getId())
                        .partName(item.getPart().getName())
                        .partCode(item.getPart().getCode())
                        .partGroup(item.getPart().getGroup().getName())
                        .partCategory(item.getPart().getGroup().getCategory().getName())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(partOrder.getStatus().name())
                .factoryId(partOrder.getFactory().getId())
                .factoryName(partOrder.getFactory().getName())
                .items(itemDtos)
                .build();
    }

    // 주문에 필요한 자재 계산 메서드
    private Map<Long, Long> calculateRequiredMaterials(PartOrderRequestDto request) {
        Map<Long, Long> materialQuantities = new HashMap<>();

        for (PartOrderRequestDto.PartOrderItemRequestDto item : request.getItems()) {
            Bom bom = bomRepository.findByPart_Id(item.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

            for (BomMaterial bomMaterial : bom.getMaterials()) {
                Long materialId = bomMaterial.getMaterial().getId();
                Long requiredQty = bomMaterial.getQuantity() * item.getQuantity();
                materialQuantities.put(materialId,
                        materialQuantities.getOrDefault(materialId, 0L) + requiredQty);
            }
        }

        return materialQuantities;
    }

    // 최적의 공장 선택 메서드
    private Factory findOptimalFactory(Map<Long, Long> requiredMaterials, String warehouseName) {
        // 모든 공장 조회
        List<Factory> factories = factoryRepository.findAll();

        // 조건에 맞는 최적의 공장 찾기
        return factories.stream()
                .filter(factory -> hasEnoughMaterials(factory.getId(), requiredMaterials))
                // 필요시 위치 기반 필터링 추가 (창고와의 거리 등)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(ErrorStatus.NO_AVAILABLE_FACTORY));
    }

    // 공장의 자재 재고 충분 여부 확인
    private boolean hasEnoughMaterials(Long factoryId, Map<Long, Long> requiredMaterials) {
        for (Map.Entry<Long, Long> entry : requiredMaterials.entrySet()) {
            Long materialId = entry.getKey();
            Long requiredQty = entry.getValue();

            FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findByFactoryIdAndMaterialId(factoryId, materialId)
                    .orElse(null);

            if (factoryMaterial == null || factoryMaterial.getQuantity() < requiredQty) {
                return false;
            }
        }
        return true;
    }

}
