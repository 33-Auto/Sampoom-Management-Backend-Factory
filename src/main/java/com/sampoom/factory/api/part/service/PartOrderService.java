package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.bom.entity.Bom;
import com.sampoom.factory.api.bom.entity.BomMaterial;
import com.sampoom.factory.api.bom.repository.BomRepository;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.Part;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.entity.PartOrderItem;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.entity.PartOrderPriority;
import com.sampoom.factory.api.part.entity.MaterialAvailability;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import com.sampoom.factory.api.part.repository.PartRepository;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import com.sampoom.factory.common.response.PageResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PartOrderService {
    private final BomRepository bomRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final PartRepository partRepository;
    private final PartOrderRepository partOrderRepository;
    private final FactoryRepository factoryRepository;

    // 새로운 주문 흐름: 검토중 -> MRP 실행 -> 구매요청/계획확정 -> 진행중 -> 완료
    @Transactional
    public PartOrderResponseDto createPartOrder(PartOrderRequestDto request) {
        // 주문에 필요한 자재 계산
        Map<Long, Long> requiredMaterials = calculateRequiredMaterials(request);

        // 적절한 공장 선택 (자재 재고나 위치에 따라)
        Factory factory = findOptimalFactory(requiredMaterials, request.getWarehouseName());

        // 주문 생성 (초기 상태: 검토중)
        PartOrder partOrder = PartOrder.builder()
                .factory(factory)
                .status(PartOrderStatus.UNDER_REVIEW)
                .warehouseName(request.getWarehouseName())
                .orderDate(LocalDateTime.now())
                .requiredDate(request.getRequiredDate()) // 고객 요청 필요일 설정
                .build();

        List<PartOrderItem> items = new ArrayList<>();
        for (PartOrderRequestDto.PartOrderItemRequestDto itemReq : request.getItems()) {
            Part part = partRepository.findById(itemReq.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));

            items.add(PartOrderItem.builder()
                    .partOrder(partOrder)
                    .part(part)
                    .quantity(itemReq.getQuantity())
                    .build());
        }

        partOrder.getItems().addAll(items);

        // 우선순위 계산 및 설정
        partOrder.calculateAndSetPriority();

        // 초기 자재가용성 확인 및 설정
        boolean materialShortage = checkInitialMaterialAvailability(partOrder);
        partOrder.updateMaterialAvailability(
            materialShortage ? MaterialAvailability.INSUFFICIENT : MaterialAvailability.SUFFICIENT
        );

        partOrderRepository.save(partOrder);

        // 주문 생성 시에는 MRP를 자동으로 실행하지 않음 (별도 API로 분리)
        log.info("부품 주문 생성 완료 - 주문 ID: {}, 상태: {}, 우선순위: {}, 자재가용성: {}",
            partOrder.getId(), partOrder.getStatus(), partOrder.getPriority(), partOrder.getMaterialAvailability());

        return toResponseDto(partOrder);
    }

    // 초기 자재가용성 확인 (간단한 체크)
    private boolean checkInitialMaterialAvailability(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            Bom bom = bomRepository.findByPart_Id(item.getPart().getId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

            for (BomMaterial bomMaterial : bom.getMaterials()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterial().getId())
                        .orElse(null);

                long required = bomMaterial.getQuantity() * item.getQuantity();

                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    return true; // 자재 부족
                }
            }
        }
        return false; // 자재 충분
    }

    // MRP 실행 API (별도 분리)
    @Transactional
    public PartOrderResponseDto executeMRP(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (partOrder.getStatus() != PartOrderStatus.UNDER_REVIEW) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        executeMRPLogic(partOrder);
        return toResponseDto(partOrder);
    }

    // MRP 실행 로직
    @Transactional
    public void executeMRPLogic(PartOrder partOrder) {
        log.info("MRP 실행 시작 - 주문 ID: {}", partOrder.getId());

        // 부품별 리드타임을 고려한 생산 소요 시간 계산
        int maxProductionLeadTime = calculateProductionLeadTime(partOrder);

        // 자재 부족 여부 확인 및 자재 조달 리드타임 계산
        MaterialAvailabilityResult materialResult = checkMaterialAvailabilityWithLeadTime(partOrder);

        // 최종 예정일 계산 (현재 시점에서 최대 리드타임만큼 추가)
        int totalLeadTimeDays = Math.max(maxProductionLeadTime, materialResult.getMaxMaterialLeadTime());
        LocalDateTime scheduledDate = LocalDateTime.now().plusDays(totalLeadTimeDays);
        partOrder.updateScheduledDate(scheduledDate);

        if (materialResult.isMaterialShortage()) {
            // 자재 부족 시 구매요청 상태
            partOrder.requestPurchase();
            partOrder.updateMaterialAvailability(MaterialAvailability.INSUFFICIENT);
            log.info("자재 부족으로 구매요청 상태로 변경 - 주문 ID: {}, 자재 조달 리드타임: {}일",
                partOrder.getId(), materialResult.getMaxMaterialLeadTime());
        } else {
            // 자재 충분 시 계획 확정
            partOrder.confirmPlan();
            partOrder.updateMaterialAvailability(MaterialAvailability.SUFFICIENT);
            log.info("계획 확정 완료 - 주문 ID: {}, 예정일: {}, 생산 리드타임: {}일",
                partOrder.getId(), scheduledDate, maxProductionLeadTime);

            // 예정일이 요청 필요일보다 늦으면 반려, 그렇지 않으면 계획 확정 상태 유지
            if (partOrder.getScheduledDate().isAfter(partOrder.getRequiredDate())) {
                partOrder.reject("예정일이 요청 필요일을 초과하여 주문을 반려합니다. (요청일: " +
                    partOrder.getRequiredDate() + ", 예정일: " + partOrder.getScheduledDate() + ")");
                log.info("예정일 초과로 주문 반려 - 주문 ID: {}, 요청일: {}, 예정일: {}",
                    partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate());
            } else {
                log.info("계획 확정 상태 유지 - 주문 ID: {}, 생산지시 대기중", partOrder.getId());
            }
        }

        partOrderRepository.save(partOrder);
    }

    // 부품별 리드타임을 고려한 생산 소요 시간 계산
    private int calculateProductionLeadTime(PartOrder partOrder) {
        int maxLeadTime = 0;

        for (PartOrderItem item : partOrder.getItems()) {
            Part part = item.getPart();
            // 부품의 리드타임이 없으면 기본값 사용
            int partLeadTime = part.getLeadTimeDays() != null ? part.getLeadTimeDays() : 3;

            // 수량에 따른 추가 시간 계산 (대량 생산 시 추가 시간 필요)
            int quantityFactor = (int) Math.ceil(item.getQuantity() / 100.0); // 100개당 1일 추가
            int totalPartLeadTime = partLeadTime + quantityFactor;

            // 가장 긴 리드타임을 사용 (병렬 생산 가정)
            maxLeadTime = Math.max(maxLeadTime, totalPartLeadTime);

            log.debug("부품 {} 리드타임: {}일, 수량: {}, 총 리드타임: {}일",
                part.getName(), partLeadTime, item.getQuantity(), totalPartLeadTime);
        }

        return Math.max(maxLeadTime, 1); // 최소 1일
    }

    // 자재 가용성 확인 및 리드타임 계산
    private MaterialAvailabilityResult checkMaterialAvailabilityWithLeadTime(PartOrder partOrder) {
        boolean materialShortage = false;
        int maxMaterialLeadTime = 0;

        for (PartOrderItem item : partOrder.getItems()) {
            Bom bom = bomRepository.findByPart_Id(item.getPart().getId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

            for (BomMaterial bomMaterial : bom.getMaterials()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterial().getId())
                        .orElse(null);

                long required = bomMaterial.getQuantity() * item.getQuantity();

                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    materialShortage = true;

                    // 부족한 자재의 리드타임 계산
                    Material material = bomMaterial.getMaterial();
                    int materialLeadTime = material.getLeadTimeDays() != null ? material.getLeadTimeDays() : 7; // 기본 7일
                    maxMaterialLeadTime = Math.max(maxMaterialLeadTime, materialLeadTime);

                    log.debug("자재 부족 - 자재명: {}, 필요수량: {}, 재고수량: {}, 리드타임: {}일",
                        material.getName(), required,
                        factoryMaterial != null ? factoryMaterial.getQuantity() : 0,
                        materialLeadTime);
                }
            }
        }

        return new MaterialAvailabilityResult(materialShortage, maxMaterialLeadTime);
    }

    // 자재 가용성 결과를 담는 내부 클래스
    private static class MaterialAvailabilityResult {
        private final boolean materialShortage;
        private final int maxMaterialLeadTime;

        public MaterialAvailabilityResult(boolean materialShortage, int maxMaterialLeadTime) {
            this.materialShortage = materialShortage;
            this.maxMaterialLeadTime = maxMaterialLeadTime;
        }

        public boolean isMaterialShortage() {
            return materialShortage;
        }

        public int getMaxMaterialLeadTime() {
            return maxMaterialLeadTime;
        }
    }


    // 주문 완료 처리
    @Transactional
    public PartOrderResponseDto completePartOrder(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (partOrder.getStatus() != PartOrderStatus.IN_PROGRESS) {
            throw new BadRequestException(ErrorStatus.ORDER_NOT_IN_PROGRESS);
        }

        partOrder.complete();
        partOrderRepository.save(partOrder);

        return toResponseDto(partOrder);
    }

    // 주문 상태 업데이트 (진행률 갱신)
    @Transactional
    public PartOrderResponseDto updatePartOrderProgress(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // 진행률과 D-day 계산
        partOrder.calculateProgressByDate();
        partOrderRepository.save(partOrder);

        return toResponseDto(partOrder);
    }

    // 주문 조회
    public PartOrderResponseDto getPartOrder(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (!partOrder.getFactory().getId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.INVALID_FACTORY_FOR_PART_ORDER);
        }

        // 조회 시 진행률 업데이트
        partOrder.calculateProgressByDate();

        return toResponseDto(partOrder);
    }

    // 주문 목록 조회
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
                    // 각 주문의 진행률 업데이트
                    partOrder.calculateProgressByDate();
                    return toResponseDto(partOrder);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    // DTO 변환 메서드
    private PartOrderResponseDto toResponseDto(PartOrder partOrder) {
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
                .factoryId(partOrder.getFactory().getId())
                .factoryName(partOrder.getFactory().getName())
                .requiredDate(partOrder.getRequiredDate())
                .scheduledDate(partOrder.getScheduledDate())
                .progressRate(partOrder.getProgressRate())
                .rejectionReason(partOrder.getRejectionReason())
                .dDay(partOrder.getDDay())
                .priority(partOrder.getPriority() != null ? partOrder.getPriority().name() : null)
                .materialAvailability(partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null)
                .items(itemDtos)
                .build();
    }

    private Map<Long, Long> calculateRequiredMaterials(PartOrderRequestDto request) {
        Map<Long, Long> materialQuantities = new HashMap<>();

        for (PartOrderRequestDto.PartOrderItemRequestDto item : request.getItems()) {
            Bom bom = bomRepository.findByPart_Id(item.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

            for (BomMaterial bomMaterial : bom.getMaterials()) {
                Long materialId = bomMaterial.getMaterial().getId();
                Long requiredQuantity = bomMaterial.getQuantity() * item.getQuantity();
                materialQuantities.merge(materialId, requiredQuantity, Long::sum);
            }
        }

        return materialQuantities;
    }

    private Factory findOptimalFactory(Map<Long, Long> requiredMaterials, String warehouseName) {
        List<Factory> factories = factoryRepository.findAll();

        Factory optimalFactory = null;
        int bestScore = -1;

        for (Factory factory : factories) {
            int score = 0;
            boolean canProduce = true;

            // 자재 재고량 확인
            for (Map.Entry<Long, Long> entry : requiredMaterials.entrySet()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(factory.getId(), entry.getKey())
                        .orElse(null);

                if (factoryMaterial != null && factoryMaterial.getQuantity() >= entry.getValue()) {
                    score += 10; // 자재 충분 시 점수 추가
                } else {
                    score -= 5; // 자재 부족 시 점수 차감
                }
            }

            // 창고명과 공장 주소의 유사성 (간단한 예시)
            if (factory.getAddress() != null && factory.getAddress().contains(warehouseName)) {
                score += 20; // 위치 근접성 보너스
            }

            if (score > bestScore) {
                bestScore = score;
                optimalFactory = factory;
            }
        }

        if (optimalFactory == null) {
            // 적절한 공장이 없으면 첫 번째 공장 선택
            optimalFactory = factories.get(0);
        }

        return optimalFactory;
    }

    // 생산지시 API (계획확정 상태에서 진행중으로 변경)
    @Transactional
    public PartOrderResponseDto startProduction(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        // 진행중 상태로 변경
        partOrder.startProgress();

        // 자재 차감
        deductMaterials(partOrder);

        partOrderRepository.save(partOrder);

        log.info("생산지시 완료 - 주문 ID: {}, 상태: {} -> {}",
            partOrder.getId(), PartOrderStatus.PLAN_CONFIRMED, PartOrderStatus.IN_PROGRESS);

        return toResponseDto(partOrder);
    }

    // 자재 차감
    private void deductMaterials(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            Bom bom = bomRepository.findByPart_Id(item.getPart().getId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));

            for (BomMaterial bomMaterial : bom.getMaterials()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterial().getId())
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));

                long required = bomMaterial.getQuantity() * item.getQuantity();
                factoryMaterial.decreaseQuantity(required);
            }
        }
    }
}
