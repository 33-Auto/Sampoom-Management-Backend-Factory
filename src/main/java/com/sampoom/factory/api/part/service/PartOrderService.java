package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.bom.entity.BomProjection;
import com.sampoom.factory.api.bom.entity.BomMaterialProjection;
import com.sampoom.factory.api.bom.repository.BomProjectionRepository;
import com.sampoom.factory.api.bom.repository.BomMaterialProjectionRepository;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.*;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import com.sampoom.factory.api.part.repository.PartProjectionRepository;
import com.sampoom.factory.api.purchase.dto.PurchaseRequestDto;
import com.sampoom.factory.api.purchase.service.PurchaseRequestService;
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
    private final BomProjectionRepository bomProjectionRepository;
    private final BomMaterialProjectionRepository bomMaterialProjectionRepository;
    private final FactoryMaterialRepository factoryMaterialRepository;
    private final PartProjectionRepository partProjectionRepository;
    private final PartOrderRepository partOrderRepository;
    private final FactoryRepository factoryRepository;
    private final MaterialProjectionRepository materialProjectionRepository;
    private final PurchaseRequestService purchaseRequestService; // 구매요청 서비스 추가
    private final PartOrderCodeGenerator partOrderCodeGenerator; // 주문 코드 생성기 추가

    // 새로운 주문 흐름: 검토중 -> MRP 실행 -> 구매요청/계획확정 -> 진행중 -> 완료
    @Transactional
    public PartOrderResponseDto createPartOrder(PartOrderRequestDto request) {
        // 주문에 필요한 자재 계산
        Map<Long, Long> requiredMaterials = calculateRequiredMaterials(request);

        // 적절한 공장 선택 (자재 재고나 위치에 따라)
        Factory factory = findOptimalFactory(requiredMaterials, request.getWarehouseName());

        // 주문 코드 자동 생성
        String orderCode = partOrderCodeGenerator.generateOrderCode();

        // 주문 생성 (초기 상태: 검토중)
        PartOrder partOrder = PartOrder.builder()
                .factory(factory)
                .status(PartOrderStatus.UNDER_REVIEW)
                .warehouseName(request.getWarehouseName())
                .orderDate(LocalDateTime.now())
                .requiredDate(request.getRequiredDate()) // 고객 요청 필요일 설정
                .orderCode(orderCode) // 자동 생성된 주문 코드 설정
                .build();

        List<PartOrderItem> items = new ArrayList<>();
        for (PartOrderRequestDto.PartOrderItemRequestDto itemReq : request.getItems()) {
            // PartProjection만 사용, partId만 저장
            items.add(PartOrderItem.builder()
                    .partOrder(partOrder)
                    .partId(itemReq.getPartId())
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

        // 주문 생성 시��는 MRP를 자동으로 실행하지 않음 (별도 API로 분리)
        log.info("부품 주문 생성 완료 - 주문 ID: {}, 상태: {}, 우선순위: {}, 자재가용성: {}",
            partOrder.getId(), partOrder.getStatus(), partOrder.getPriority(), partOrder.getMaterialAvailability());

        return toResponseDto(partOrder);
    }

    // 자재 계산 projection 기반으로 변경
    private Map<Long, Long> calculateRequiredMaterials(PartOrderRequestDto request) {
        Map<Long, Long> materialQuantities = new HashMap<>();
        for (PartOrderRequestDto.PartOrderItemRequestDto item : request.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                Long materialId = bomMaterial.getMaterialId();
                Long requiredQuantity = (long) bomMaterial.getQuantity() * item.getQuantity();
                materialQuantities.merge(materialId, requiredQuantity, Long::sum);
            }
        }
        return materialQuantities;
    }

    // 초기 자재가용성 확인 (간단한 체크)
    private boolean checkInitialMaterialAvailability(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterialId())
                    .orElse(null);
                long required = (long) bomMaterial.getQuantity() * item.getQuantity();
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

        // 자재가용성 설정
        partOrder.updateMaterialAvailability(
            materialResult.isMaterialShortage() ? MaterialAvailability.INSUFFICIENT : MaterialAvailability.SUFFICIENT
        );

        // 예정일과 요청일 비교하여 상태 결정
        if (partOrder.getScheduledDate().isAfter(partOrder.getRequiredDate())) {
            // 예정일이 요청일보다 늦으면 지연 상태
            partOrder.markAsDelayed();
            log.info("예정일이 요청일을 초과하여 지연 상태로 설정 - 주문 ID: {}, 요청일: {}, 예정일: {}",
                partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate());
        } else {
            // 예정일이 요청일보다 빠르거나 같으면 계획확정
            partOrder.confirmPlan();
            log.info("예정일이 요청일 이내로 계획확정 상태로 설정 - 주문 ID: {}, 요청일: {}, 예정일: {}",
                partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate());
        }

        if (materialResult.isMaterialShortage()) {
            log.info("자재 부족 감지 - 주문 ID: {}, 자재 조달 리드타임: {}일",
                partOrder.getId(), materialResult.getMaxMaterialLeadTime());
        } else {
            log.info("자재 충분 - 주문 ID: {}, 생산 리드타임: {}일",
                partOrder.getId(), maxProductionLeadTime);
        }

        partOrderRepository.save(partOrder);
    }

    // 부품별 리드타임을 고려한 생산 소요 시간 계산
    private int calculateProductionLeadTime(PartOrder partOrder) {
        int maxLeadTime = 0;
        for (PartOrderItem item : partOrder.getItems()) {
            // PartProjection만 사용
            PartProjection part = partProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));
            int partLeadTime = part.getLeadTime() != null ? part.getLeadTime() : 3;
            int quantityFactor = (int) Math.ceil(item.getQuantity() / 100.0);
            int totalPartLeadTime = partLeadTime + quantityFactor;
            maxLeadTime = Math.max(maxLeadTime, totalPartLeadTime);
            log.info("부품 {} 리드타임: {}일, 수량: {}, 총 리드타임: {}일",
                part.getName(), partLeadTime, item.getQuantity(), totalPartLeadTime);
        }
        return Math.max(maxLeadTime, 1);
    }

    // 자재 가용성 확인 및 리드타임 계산 (projection 기반, 리드타임은 기본값 7일)
    private MaterialAvailabilityResult checkMaterialAvailabilityWithLeadTime(PartOrder partOrder) {
        boolean materialShortage = false;
        int maxMaterialLeadTime = 0;

        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterialId())
                        .orElse(null);

                long required = (long) bomMaterial.getQuantity() * item.getQuantity();

                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    materialShortage = true;
                    int materialLeadTime = materialProjectionRepository.findByMaterialId(bomMaterial.getMaterialId())
                        .map(mp -> mp.getLeadTime())
                        .orElse(7); // projection이 없으면 기본값 7일
                    maxMaterialLeadTime = Math.max(maxMaterialLeadTime, materialLeadTime);
                    // 자재 이름도 함께 로그로 출력
                    String materialName = materialProjectionRepository.findByMaterialId(bomMaterial.getMaterialId())
                        .map(mp -> mp.getName())
                        .orElse("UNKNOWN");
                    log.info("자재 부족 - 자재ID: {}, 자재명: {}, 필요수량: {}, 재고수량: {}, 리드타임: {}일",
                        bomMaterial.getMaterialId(), materialName, required,
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
                .map(item -> {
                    PartProjection part = partProjectionRepository.findByPartId(item.getPartId())
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));
                    return PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(part.getPartId())
                        .partName(part.getName())
                        .partCode(part.getCode())
                        .partGroup(part.getGroupId() != null ? String.valueOf(part.getGroupId()) : null)
                        .partCategory(part.getCategoryId() != null ? String.valueOf(part.getCategoryId()) : null)
                        .quantity(item.getQuantity())
                        .build();
                })
                .toList();

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .orderCode(partOrder.getOrderCode()) // 주문 코드 추가
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

        // PLAN_CONFIRMED 또는 DELAYED 상태에서 생산지시 가능
        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED &&
            partOrder.getStatus() != PartOrderStatus.DELAYED) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        // 진행중 상태로 변경
        partOrder.startProgress();

        // 자재 차감 (자재가 충분한 경우에만)
        if (partOrder.getMaterialAvailability() == MaterialAvailability.SUFFICIENT) {
            deductMaterials(partOrder);
            log.info("생산지시 완료 및 자재 차감 - 주문 ID: {}", partOrder.getId());
        } else {
            log.info("자재 부족으로 자재 차감 없이 생산지시만 완료 - 주문 ID: {}", partOrder.getId());
        }

        partOrderRepository.save(partOrder);

        return toResponseDto(partOrder);
    }

    // MRP 결과 적용 API (클라이언트의 "결과적용" 버튼)
    @Transactional
    public PartOrderResponseDto applyMRPResult(Long factoryId, Long orderId) {
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryId(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // PLAN_CONFIRMED 또는 DELAYED 상태에서만 결과 적용 가능
        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED &&
            partOrder.getStatus() != PartOrderStatus.DELAYED) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        if (partOrder.getMaterialAvailability() == MaterialAvailability.INSUFFICIENT) {
            // 자재 부족 시: 자재 구매요청 + 생산지시
            log.info("자재 부족으로 구매요청 및 생산지시 동시 진행 - 주문 ID: {}", partOrder.getId());

            // 1. 자재 구매요청 처리
            requestMaterialPurchase(partOrder);

            // 2. 생산지시 처리 (부족한 자재는 구매 예정이므로 생산 준비)
            partOrder.startProgress();
            log.info("자재 구매요청과 함께 생산지시 완료 - 주문 ID: {}", partOrder.getId());

        } else {
            // 자재 충분 시: 생산지시만
            log.info("자재 충분으로 생산지시만 진행 - 주문 ID: {}", partOrder.getId());

            // 생산지시 처리
            partOrder.startProgress();
            // 자재 차감
            deductMaterials(partOrder);
            log.info("생산지시 완료 및 자재 차감 - 주문 ID: {}", partOrder.getId());
        }

        partOrderRepository.save(partOrder);
        return toResponseDto(partOrder);
    }

    // 자재 구매요청 처리 (자재 부족 시 호출) - 실제 API 호출로 변경
    private void requestMaterialPurchase(PartOrder partOrder) {
        log.info("자재 구매요청 처리 시작 - 주문 ID: {}", partOrder.getId());

        List<PurchaseRequestDto.PurchaseItemDto> purchaseItems = new ArrayList<>();

        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());

            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterialId())
                    .orElse(null);

                long required = (long) bomMaterial.getQuantity() * item.getQuantity();
                long currentStock = factoryMaterial != null ? factoryMaterial.getQuantity() : 0;

                if (currentStock < required) {
                    long shortageAmount = required - currentStock;
                    var materialProjection = materialProjectionRepository.findByMaterialId(bomMaterial.getMaterialId());

                    String materialCode = materialProjection.map(mp -> mp.getCode()).orElse("MTL-" + bomMaterial.getMaterialId());
                    String materialName = materialProjection.map(mp -> mp.getName()).orElse("UNKNOWN");
                    String unit = materialProjection.map(mp -> mp.getMaterialUnit()).orElse("EA"); // materialUnit 필드 사용
                    Long unitPrice = 1000L; // 기본 단가 (MaterialProjection에 unitPrice 필드가 없으므로 고정값 사용)

                    // 구매요청 아이템 추가
                    purchaseItems.add(PurchaseRequestDto.PurchaseItemDto.builder()
                            .materialCode(materialCode)
                            .materialName(materialName)
                            .unit(unit)
                            .quantity(shortageAmount)
                            .unitPrice(unitPrice)
                            .build());

                    log.info("자재 구매요청 아이템 추가 - 자재코드: {}, 자재명: {}, 부족수량: {}, 단가: {}",
                        materialCode, materialName, shortageAmount, unitPrice);
                }
            }
        }

        // 구매요청 아이템이 있는 경우에만 API 호출
        if (!purchaseItems.isEmpty()) {
            PurchaseRequestDto purchaseRequest = PurchaseRequestDto.builder()
                    .factoryId(partOrder.getFactory().getId())
                    .factoryName(partOrder.getFactory().getName())
                    .requiredAt(partOrder.getRequiredDate().toLocalDate()) // 필요일로 설정
                    .requesterName("MRP 시스템") // 시스템 자동 요청
                    .items(purchaseItems)
                    .build();

            // 외부 구매요청 API 호출
            purchaseRequestService.sendPurchaseRequest(purchaseRequest);

            log.info("자재 구매요청 API 호출 완료 - 주문 ID: {}, 구매아이템 수: {}",
                partOrder.getId(), purchaseItems.size());
        } else {
            log.info("구매요청할 자재가 없습니다 - 주문 ID: {}", partOrder.getId());
        }

        log.info("자재 구매요청 처리 완료 - 주문 ID: {}", partOrder.getId());
    }

    // 자재 차감 projection 기반으로 변경
    private void deductMaterials(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findByFactoryIdAndMaterialId(partOrder.getFactory().getId(), bomMaterial.getMaterialId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                long required = (long) bomMaterial.getQuantity() * item.getQuantity();
                factoryMaterial.decreaseQuantity(required);
            }
        }
    }
}
