package com.sampoom.factory.api.material.service;

import com.sampoom.factory.common.response.PageResponseDto;
import com.sampoom.factory.api.factory.entity.FactoryProjection;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.factory.repository.FactoryProjectionRepository;
import com.sampoom.factory.api.material.entity.OrderStatus;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.material.entity.MaterialProjection;
import com.sampoom.factory.api.material.dto.MaterialOrderRequestDto;
import com.sampoom.factory.api.material.dto.MaterialOrderResponseDto;
import com.sampoom.factory.api.material.entity.MaterialOrder;
import com.sampoom.factory.api.material.entity.MaterialOrderItem;
import com.sampoom.factory.api.material.repository.MaterialOrderItemRepository;
import com.sampoom.factory.api.material.repository.MaterialOrderRepository;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialOrderService {

    private final MaterialOrderRepository orderRepository;
    private final MaterialOrderItemRepository orderItemRepository;
    private final FactoryProjectionRepository factoryProjectionRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final MaterialProjectionRepository materialProjectionRepository;

    @Transactional
    public MaterialOrderResponseDto createMaterialOrder(Long factoryId, MaterialOrderRequestDto requestDto) {
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        MaterialOrder order = MaterialOrder.builder()
                .code(generateOrderCode())
                .factoryId(factoryId)
                .status(OrderStatus.ORDERED)
                .orderAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        List<MaterialOrderItem> orderItems = requestDto.getItems().stream()
                .map(item -> {
                    MaterialProjection materialProjection = materialProjectionRepository.findByMaterialId(item.getMaterialId())
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                    return MaterialOrderItem.builder()
                            .materialOrder(order)
                            .materialId(materialProjection.getMaterialId())
                            .quantity(item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        MaterialOrderResponseDto response = MaterialOrderResponseDto.from(order, orderItems, materialId ->
            materialProjectionRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND))
        );

        // factoryName 설정
        response = MaterialOrderResponseDto.builder()
                .id(response.getId())
                .code(response.getCode())
                .factoryId(response.getFactoryId())
                .factoryName(factory.getBranchName())
                .status(response.getStatus())
                .orderAt(response.getOrderAt())
                .receivedAt(response.getReceivedAt())
                .items(response.getItems())
                .build();

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<MaterialOrderResponseDto> getMaterialOrdersByFactory(Long factoryId, int page, int size) {
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MaterialOrder> ordersPage = orderRepository.findByFactoryId(factoryId, pageRequest);

        List<MaterialOrderResponseDto> content = ordersPage.getContent().stream()
                .map(order -> {
                    List<MaterialOrderItem> items = orderItemRepository.findByMaterialOrderId(order.getId());
                    MaterialOrderResponseDto response = MaterialOrderResponseDto.from(order, items, materialId ->
                        materialProjectionRepository.findByMaterialId(materialId)
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND))
                    );

                    // factoryName 설정
                    return MaterialOrderResponseDto.builder()
                            .id(response.getId())
                            .code(response.getCode())
                            .factoryId(response.getFactoryId())
                            .factoryName(factory.getBranchName())
                            .status(response.getStatus())
                            .orderAt(response.getOrderAt())
                            .receivedAt(response.getReceivedAt())
                            .items(response.getItems())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResponseDto.<MaterialOrderResponseDto>builder()
                .content(content)
                .totalElements(ordersPage.getTotalElements())
                .totalPages(ordersPage.getTotalPages())
                .build();
    }

    @Transactional
    public MaterialOrderResponseDto receiveMaterialOrder(Long factoryId, Long orderId) {
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        MaterialOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));

        if (!order.getFactoryId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.FACTORY_MATERIAL_ORDER_MISMATCH);
        }

        order.receive();
        orderRepository.save(order);

        // 주문 아이템 조회
        List<MaterialOrderItem> items = orderItemRepository.findByMaterialOrderId(orderId);

        // 각 주문 아이템에 대해 공장 자재 수량 증가
        for (MaterialOrderItem item : items) {
            Long materialId = item.getMaterialId();
            Long quantity = item.getQuantity();

            // 해당 공장의 자재 찾기
            FactoryMaterial factoryMaterial = factoryMaterialRepository.findByFactoryIdAndMaterialId(
                            factoryId, materialId)
                    .orElseGet(() -> {
                        // 없으면 새로 생성
                        FactoryMaterial newMaterial = FactoryMaterial.builder()
                                .factoryId(factoryId)
                                .materialId(materialId)
                                .quantity(0L)
                                .build();
                        return factoryMaterialRepository.save(newMaterial);
                    });

            // 수량 증가
            factoryMaterial.increaseQuantity(quantity);
        }

        // factoryName이 포함된 응답 생성
        MaterialOrderResponseDto response = MaterialOrderResponseDto.from(order, items, materialId ->
            materialProjectionRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND))
        );

        return MaterialOrderResponseDto.builder()
                .id(response.getId())
                .code(response.getCode())
                .factoryId(response.getFactoryId())
                .factoryName(factory.getBranchName())
                .status(response.getStatus())
                .orderAt(response.getOrderAt())
                .receivedAt(response.getReceivedAt())
                .items(response.getItems())
                .build();
    }

    @Transactional
    public MaterialOrderResponseDto cancelMaterialOrder(Long factoryId, Long orderId) {
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        MaterialOrder order = orderRepository
                .findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        order.cancel();

        List<MaterialOrderItem> items = orderItemRepository.findByMaterialOrderId(orderId);
        MaterialOrderResponseDto response = MaterialOrderResponseDto.from(order, items, materialId ->
            materialProjectionRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND))
        );

        return MaterialOrderResponseDto.builder()
                .id(response.getId())
                .code(response.getCode())
                .factoryId(response.getFactoryId())
                .factoryName(factory.getBranchName())
                .status(response.getStatus())
                .orderAt(response.getOrderAt())
                .receivedAt(response.getReceivedAt())
                .items(response.getItems())
                .build();
    }

    @Transactional
    public void softDeleteMaterialOrder(Long factoryId, Long orderId) {
        MaterialOrder order = orderRepository
                .findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));

        // JPA delete() 호출 → @SQLDelete가 UPDATE로 변환
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public MaterialOrderResponseDto getMaterialOrderDetail(Long factoryId, Long orderId) {
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        MaterialOrder order = orderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        List<MaterialOrderItem> items = orderItemRepository.findByMaterialOrderId(orderId);

        MaterialOrderResponseDto response = MaterialOrderResponseDto.from(order, items, materialId ->
            materialProjectionRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND))
        );

        return MaterialOrderResponseDto.builder()
                .id(response.getId())
                .code(response.getCode())
                .factoryId(response.getFactoryId())
                .factoryName(factory.getBranchName())
                .status(response.getStatus())
                .orderAt(response.getOrderAt())
                .receivedAt(response.getReceivedAt())
                .items(response.getItems())
                .build();
    }

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis();
    }
}

