package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.bom.entity.BomProjection;
import com.sampoom.factory.api.bom.entity.BomMaterialProjection;
import com.sampoom.factory.api.bom.repository.BomProjectionRepository;
import com.sampoom.factory.api.bom.repository.BomMaterialProjectionRepository;
import com.sampoom.factory.api.factory.entity.FactoryProjection;
import com.sampoom.factory.api.factory.repository.FactoryProjectionRepository;
import com.sampoom.factory.api.factory.entity.BranchFactoryDistance;
import com.sampoom.factory.api.factory.repository.BranchFactoryDistanceRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialProjectionRepository;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.*;
import com.sampoom.factory.api.part.repository.PartCategoryProjectionRepository;
import com.sampoom.factory.api.part.repository.PartGroupProjectionRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final FactoryProjectionRepository factoryProjectionRepository;
    private final MaterialProjectionRepository materialProjectionRepository;
    private final PurchaseRequestService purchaseRequestService; // 구매요청 서비스 추가
    private final PartOrderCodeGenerator partOrderCodeGenerator; // 주문 코드 생성기 추가
    private final BranchFactoryDistanceRepository branchFactoryDistanceRepository; // 거리 정보 Repository 추가
    private final PartOrderEventService partOrderEventService; // 이벤트 서비스 추가
    private final PartCategoryProjectionRepository partCategoryProjectionRepository; // 카테고리 Repository 추가
    private final PartGroupProjectionRepository partGroupProjectionRepository; // 그룹 Repository 추가

    // 새로운 주문 흐름: 검토중 -> MRP 실행 -> 구매요청/계획확정 -> 진행중 -> 완료
    @Transactional
    public PartOrderResponseDto createPartOrder(PartOrderRequestDto request) {
        // 주문에 필요한 자재 계산
        Map<Long, Long> requiredMaterials = calculateRequiredMaterials(request);

        // 적절한 공장 선택 (자재 재고나 위치에 따라)
        FactoryProjection factory = findOptimalFactory(requiredMaterials, request.getWarehouseId(), request.getWarehouseName());

        // 주문 코드 자동 생성
        String orderCode = partOrderCodeGenerator.generateOrderCode();

        // 주문 생성 (초기 상태: 검토중)
        PartOrder partOrder = PartOrder.builder()
                .factoryId(factory.getBranchId())
                .warehouseId(request.getWarehouseId())
                .status(PartOrderStatus.UNDER_REVIEW)
                .warehouseName(request.getWarehouseName())
                .orderDate(LocalDateTime.now())
                .requiredDate(request.getRequiredDate()) // 고객 요청 필요일 설정
                .orderCode(orderCode) // 자동 생성된 주문 코드 설정
                .externalPartOrderId(request.getExternalPartOrderId()) // 외부 주문 ID 설정
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


        // 주문 생성 시에는 MRP를 자동으로 실행하지 않음 (별도 API로 분리)
        log.info("부품 주문 생성 완료 - 주문 ID: {}, 상태: {}, 우선순위: {}, 자재가용성: {}, 외부주문ID: {}",
            partOrder.getId(), partOrder.getStatus(), partOrder.getPriority(), partOrder.getMaterialAvailability(), request.getExternalPartOrderId());

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
                Long requiredQuantity = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 Long으로 변환
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
                    .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                    .orElse(null);
                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환
                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    return true; // 자재 부족
                }
            }
        }
        return false; // 자재 충분
    }

    // MRP 실행 API (별도 분리) - 동시성 제어 ��가
    @Transactional
    public PartOrderResponseDto executeMRP(Long factoryId, Long orderId) {
        // 비관적 락을 사용하여 동시성 제어
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        if (partOrder.getStatus() != PartOrderStatus.UNDER_REVIEW) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        executeMRPLogic(partOrder);
        return toResponseDto(partOrder);
    }

    // 일괄 MRP 실행 API
    @Transactional
    public List<PartOrderResponseDto> executeBatchMRP(Long factoryId, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST);
        }

        log.info("일괄 MRP 실행 시작 - 공장 ID: {}, 주문 수: {}", factoryId, orderIds.size());

        List<PartOrderResponseDto> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long orderId : orderIds) {
            try {
                // 각 주문에 대해 개별적으로 MRP 실행
                PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

                if (partOrder.getStatus() != PartOrderStatus.UNDER_REVIEW) {
                    log.warn("MRP 실행 불가 - 주문 ID: {}, 현재 상태: {}", orderId, partOrder.getStatus());
                    failCount++;
                    continue;
                }

                executeMRPLogic(partOrder);
                results.add(toResponseDto(partOrder));
                successCount++;

                log.info("MRP 실행 완료 - 주문 ID: {}", orderId);

            } catch (Exception e) {
                log.error("MRP 실행 실패 - 주문 ID: {}, 오류: {}", orderId, e.getMessage(), e);
                failCount++;
                // 개별 실패는 전체 작업을 중단하지 않음
            }
        }

        log.info("일괄 MRP 실행 완료 - 공장 ID: {}, 성공: {}, 실패: {}", factoryId, successCount, failCount);
        return results;
    }

    // MRP 실행 로직
    @Transactional
    public void executeMRPLogic(PartOrder partOrder) {
        log.info("MRP 실행 시작 - 주문 ID: {}", partOrder.getId());

        // 디버깅: MRP 실행 시 externalPartOrderId 값 확인
        log.info("디버깅 - MRP 실행 시 externalPartOrderId: {}", partOrder.getExternalPartOrderId());

        // 부품별 리드타임을 고려한 생산 소요 시간 계산
        int maxProductionLeadTime = calculateProductionLeadTime(partOrder);

        // 자재 부족 여부 확인 및 자재 조달 리드타임 계산
        MaterialAvailabilityResult materialResult = checkMaterialAvailabilityWithLeadTime(partOrder);

        // 총 리드타임 계산: 자재 부족 시 자재 조달 + 생산, 충분 시 생산만
        int totalLeadTimeDays;
        if (materialResult.isMaterialShortage()) {
            // 자재 부족 시: 자재 조달 리드타임 + 부품 생산 리드타임
            totalLeadTimeDays = materialResult.getMaxMaterialLeadTime() + maxProductionLeadTime;
            log.info("자재 부족으로 순차 진행 - 자재 조달: {}일, 부품 생산: {}일, 총 리드타임: {}일",
                materialResult.getMaxMaterialLeadTime(), maxProductionLeadTime, totalLeadTimeDays);
        } else {
            // 자재 충분 시: 부품 생산 리드타임만
            totalLeadTimeDays = maxProductionLeadTime;
            log.info("자재 충분으로 즉시 생산 - 부품 생산: {}일, 총 리드타임: {}일",
                maxProductionLeadTime, totalLeadTimeDays);
        }

        LocalDateTime scheduledDate = LocalDateTime.now().plusDays(totalLeadTimeDays);
        partOrder.updateScheduledDate(scheduledDate);

        // 요구일에 맞추기 위한 최소 시작일 계산 (역산)
        LocalDateTime minimumStartDate = partOrder.getRequiredDate().minusDays(totalLeadTimeDays);
        partOrder.updateMinimumStartDate(minimumStartDate);

        log.info("MRP 계산 완료 - 주문 ID: {}, 총 리드타임: {}일, 예정일: {}, 최소 시작일: {}",
            partOrder.getId(), totalLeadTimeDays, scheduledDate, minimumStartDate);

        // 자재가용성 설정
        partOrder.updateMaterialAvailability(
            materialResult.isMaterialShortage() ? MaterialAvailability.INSUFFICIENT : MaterialAvailability.SUFFICIENT
        );

        // 최소 시작일이 현재 날짜보다 이전인지 확인하여 상태 결정
        LocalDateTime now = LocalDateTime.now();
        if (minimumStartDate.isBefore(now)) {
            // 최소 시작일이 이미 지났으면, 예정일과 요청일 비교하여 상태 결정
            if (partOrder.getScheduledDate().isAfter(partOrder.getRequiredDate())) {
                partOrder.markAsDelayed();
                log.warn("지연 상태 - 주문 ID: {}, 요구일: {}, 예정일: {}, 최소 시작일(이미 경과): {}",
                    partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate(), minimumStartDate);
            } else {
                partOrder.confirmPlan();
                log.info("계획 확정 - 주문 ID: {}, 요구일: {}, 예정일: {}, 최소 시작일(이미 경과): {}",
                    partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate(), minimumStartDate);
            }
        } else {
            // 최소 시작일이 아직 오지 않았으면 계획 확정
            partOrder.confirmPlan();
            log.info("계획 확정 - 주문 ID: {}, 요구일: {}, 예정일: {}, 최소 시작일: {}",
                partOrder.getId(), partOrder.getRequiredDate(), partOrder.getScheduledDate(), minimumStartDate);
        }

        if (materialResult.isMaterialShortage()) {
            log.info("자재 부족 감지 - 주문 ID: {}, 자재 조달 리드타임: {}일",
                partOrder.getId(), materialResult.getMaxMaterialLeadTime());
        } else {
            log.info("자재 충분 - 주문 ID: {}, 생산 리드타임: {}��",
                partOrder.getId(), maxProductionLeadTime);
        }

        partOrderRepository.save(partOrder);

        // MRP 실행으로 상��가 변경된 경우 이벤트 발행
        partOrderEventService.recordPartOrderStatusChanged(partOrder);
    }

    // 부품별 리드타임을 고려한 생산 소요 시간 계산
    private int calculateProductionLeadTime(PartOrder partOrder) {
        int maxLeadTime = 0;
        for (PartOrderItem item : partOrder.getItems()) {
            // PartProjection만 사용
            PartProjection part = partProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));
            int partLeadTime = part.getLeadTime() != null ? part.getLeadTime() : 3;

            // standardQuantity를 기준으로 배수 계산
            Integer standardQuantity = part.getStandardQuantity() != null ? part.getStandardQuantity() : 100;
            int multiplier = (int) Math.ceil((double) item.getQuantity() / standardQuantity);
            int totalPartLeadTime = partLeadTime * multiplier;

            maxLeadTime = Math.max(maxLeadTime, totalPartLeadTime);
            log.info("부품 {} 리드타임: {}일, 수량: {}, 기준수량: {}, 배수: {}, 총 리드타임: {}일",
                part.getName(), partLeadTime, item.getQuantity(), standardQuantity, multiplier, totalPartLeadTime);
        }
        return Math.max(maxLeadTime, 1);
    }

    // 자재 가용성 확인 및 리드타임 계산 (projection 기반, standardQuantity 기준 배수로 리드타임 계산)
    private MaterialAvailabilityResult checkMaterialAvailabilityWithLeadTime(PartOrder partOrder) {
        boolean materialShortage = false;
        int maxMaterialLeadTime = 0;

        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                        .orElse(null);

                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환

                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    materialShortage = true;
                    var materialProjection = materialProjectionRepository.findByMaterialId(bomMaterial.getMaterialId());

                    int baseMaterialLeadTime = materialProjection.map(mp -> mp.getLeadTime()).orElse(7);

                    // standardQuantity를 기준으로 배수 계산
                    Integer standardQuantity = materialProjection.map(mp -> mp.getStandardQuantity()).orElse(100);
                    long shortageAmount = required - (factoryMaterial != null ? Math.round(factoryMaterial.getQuantity()) : 0);
                    int multiplier = (int) Math.ceil((double) shortageAmount / standardQuantity);
                    int totalMaterialLeadTime = baseMaterialLeadTime * multiplier;

                    maxMaterialLeadTime = Math.max(maxMaterialLeadTime, totalMaterialLeadTime);
                    String materialName = materialProjection.map(mp -> mp.getName()).orElse("UNKNOWN");

                    log.info("자재 부족 - 자재ID: {}, 자재명: {}, 필요수량: {}, 재고수���: {}, 부족량: {}, 기본리드타임: {}일, 기준수량: {}, 배수: {}, 총 리드타임: {}일",
                        bomMaterial.getMaterialId(), materialName, required,
                        factoryMaterial != null ? factoryMaterial.getQuantity() : 0,
                        shortageAmount, baseMaterialLeadTime, standardQuantity, multiplier, totalMaterialLeadTime);
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
        // 비관적 락을 사용하여 동시성 제어
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // 이미 완료된 상태인 경우 중복 처리 방지
        if (partOrder.getStatus() == PartOrderStatus.COMPLETED) {
            log.warn("주문이 이미 완료 상태입니다 - 주문 ID: {}", partOrder.getId());
            return toResponseDto(partOrder);
        }

        if (partOrder.getStatus() != PartOrderStatus.IN_PROGRESS) {
            throw new BadRequestException(ErrorStatus.ORDER_NOT_IN_PROGRESS);
        }

        partOrder.complete();
        partOrderRepository.save(partOrder);

        // 주문 완료 이벤트 발행
        partOrderEventService.recordPartOrderCompleted(partOrder);

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

        if (!partOrder.getFactoryId().equals(factoryId)) {
            throw new BadRequestException(ErrorStatus.INVALID_FACTORY_FOR_PART_ORDER);
        }

        // 조회 시 진행률 업데이트
        partOrder.calculateProgressByDate();

        return toResponseDto(partOrder);
    }

    // 주문 목록 조회
    @Transactional
    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, PartOrderStatus status, int page, int size) {
        factoryProjectionRepository.findById(factoryId)
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
                    PartOrderStatus originalStatus = partOrder.getStatus();
                    partOrder.calculateProgressByDate();

                    // 상태가 변경된 경우 DB에 저장
                    if (!originalStatus.equals(partOrder.getStatus())) {
                        partOrderRepository.save(partOrder);
                    }

                    return toResponseDto(partOrder);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    // 주문 목록 조회 - 여러 상태 필터링 지원
    @Transactional
    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, List<PartOrderStatus> statuses, int page, int size) {
        factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<PartOrder> partOrderPage;

        if (statuses != null && !statuses.isEmpty()) {
            partOrderPage = partOrderRepository.findByFactoryIdAndStatusIn(factoryId, statuses, pageable);
        } else {
            partOrderPage = partOrderRepository.findByFactoryId(factoryId, pageable);
        }

        List<PartOrderResponseDto> content = partOrderPage.getContent().stream()
                .map(partOrder -> {
                    PartOrderStatus originalStatus = partOrder.getStatus();
                    partOrder.calculateProgressByDate();

                    // 상태가 변경된 경우 DB에 저장
                    if (!originalStatus.equals(partOrder.getStatus())) {
                        partOrderRepository.save(partOrder);
                    }

                    return toResponseDto(partOrder);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    // 주문 목록 조회 - 여러 상태와 우선순위 필터링 지원
    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities, int page, int size) {
        factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<PartOrder> partOrderPage;

        // 상태와 우선순위 조건에 따른 쿼리 선택
        if ((statuses != null && !statuses.isEmpty()) && (priorities != null && !priorities.isEmpty())) {
            partOrderPage = partOrderRepository.findByFactoryIdAndStatusInAndPriorityIn(factoryId, statuses, priorities, pageable);
        } else if (statuses != null && !statuses.isEmpty()) {
            partOrderPage = partOrderRepository.findByFactoryIdAndStatusIn(factoryId, statuses, pageable);
        } else if (priorities != null && !priorities.isEmpty()) {
            partOrderPage = partOrderRepository.findByFactoryIdAndPriorityIn(factoryId, priorities, pageable);
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

    // 주문 목록 조회 - 여러 상태와 우선순위 필터링 + 검색 지원
    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities, String query, int page, int size) {
        factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<PartOrder> partOrderPage;

        // 검색어가 있는 경우와 없는 경우를 분리하여 처리
        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = "%" + query.trim() + "%";

            // 상태와 우선순위 조건에 따른 쿼리 선택 (검색어 포함)
            if ((statuses != null && !statuses.isEmpty()) && (priorities != null && !priorities.isEmpty())) {
                partOrderPage = partOrderRepository.findByFactoryIdAndStatusInAndPriorityInWithSearch(factoryId, statuses, priorities, searchQuery, pageable);
            } else if (statuses != null && !statuses.isEmpty()) {
                partOrderPage = partOrderRepository.findByFactoryIdAndStatusInWithSearch(factoryId, statuses, searchQuery, pageable);
            } else if (priorities != null && !priorities.isEmpty()) {
                partOrderPage = partOrderRepository.findByFactoryIdAndPriorityInWithSearch(factoryId, priorities, searchQuery, pageable);
            } else {
                partOrderPage = partOrderRepository.findByFactoryIdWithSearch(factoryId, searchQuery, pageable);
            }
        } else {
            // 검색어가 없는 경우 기존 로직 사용
            if ((statuses != null && !statuses.isEmpty()) && (priorities != null && !priorities.isEmpty())) {
                partOrderPage = partOrderRepository.findByFactoryIdAndStatusInAndPriorityIn(factoryId, statuses, priorities, pageable);
            } else if (statuses != null && !statuses.isEmpty()) {
                partOrderPage = partOrderRepository.findByFactoryIdAndStatusIn(factoryId, statuses, pageable);
            } else if (priorities != null && !priorities.isEmpty()) {
                partOrderPage = partOrderRepository.findByFactoryIdAndPriorityIn(factoryId, priorities, pageable);
            } else {
                partOrderPage = partOrderRepository.findByFactoryId(factoryId, pageable);
            }
        }

        List<PartOrderResponseDto> content = partOrderPage.getContent().stream()
                .map(partOrder -> {
                    PartOrderStatus originalStatus = partOrder.getStatus();
                    partOrder.calculateProgressByDate();

                    // 상태가 변경된 경우 DB에 저장
                    if (!originalStatus.equals(partOrder.getStatus())) {
                        partOrderRepository.save(partOrder);
                    }

                    return toResponseDto(partOrder);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    // 주문 목록 조회 - 카테고리, 그룹 필터링 추가 (컨트롤러 시그니처와 정확히 맞춤)
    @Transactional
    public PageResponseDto<PartOrderResponseDto> getPartOrders(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities,
                                                              String query, Long categoryId, Long groupId, int page, int size) {
        factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<PartOrder> partOrderPage;

        // 검색어가 있는 경우와 없는 경우를 분리하여 처리
        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = "%" + query.trim() + "%";
            partOrderPage = partOrderRepository.findByFactoryIdWithFiltersAndSearch(
                factoryId, statuses, priorities, categoryId, groupId, searchQuery, pageable);
        } else {
            partOrderPage = partOrderRepository.findByFactoryIdWithFilters(
                factoryId, statuses, priorities, categoryId, groupId, pageable);
        }

        List<PartOrderResponseDto> content = partOrderPage.getContent().stream()
                .map(partOrder -> {
                    PartOrderStatus originalStatus = partOrder.getStatus();
                    partOrder.calculateProgressByDate();

                    // 상태가 변경된 경우 DB에 저장
                    if (!originalStatus.equals(partOrder.getStatus())) {
                        partOrderRepository.save(partOrder);
                    }

                    return toResponseDto(partOrder);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PartOrderResponseDto>builder()
                .content(content)
                .totalElements(partOrderPage.getTotalElements())
                .totalPages(partOrderPage.getTotalPages())
                .build();
    }

    /**
     * 생산계획 목록 조회 (계획 상태 + 최근 IN_PROGRESS로 전환된 데이터 포함) - 상태 필터 추가
     */
    public PageResponseDto<PartOrderResponseDto> getProductionPlans(Long factoryId, List<PartOrderStatus> statuses, List<PartOrderPriority> priorities,
                                                                  String query, Long categoryId, Long groupId,
                                                                  int page, int size, int includeRecentDays) {
        // 디버깅 로그 추가
        log.info("getProductionPlans 호출 - factoryId: {}, statuses: {}, priorities: {}, query: {}, categoryId: {}, groupId: {}, page: {}, size: {}, includeRecentDays: {}",
            factoryId, statuses, priorities, query, categoryId, groupId, page, size, includeRecentDays);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // 사용자가 명시적으로 상태를 지정한 경우와 그렇지 않은 경우를 구분
        boolean userSpecifiedStatus = statuses != null && !statuses.isEmpty();
        log.info("사용자 지정 상태 여부: {}", userSpecifiedStatus);

        Page<PartOrder> partOrderPage;

        if (userSpecifiedStatus) {
            // 사용자가 명시적으로 상태를 지정한 경우, previousStatus도 고려한 필터링 사용
            log.info("사용자 지정 상태로 previousStatus 고려한 필터링 사용: {}", statuses);

            if (StringUtils.hasText(query)) {
                String searchQuery = "%" + query.trim() + "%";
                log.info("검색어 포함 previousStatus 고려 쿼리 실행: {}", searchQuery);
                partOrderPage = partOrderRepository.findByFactoryIdWithFiltersAndSearchIncludingPreviousStatus(
                    factoryId, statuses, priorities, categoryId, groupId, searchQuery, pageable);
            } else {
                log.info("previousStatus 고려한 필터링 쿼리 실행");
                partOrderPage = partOrderRepository.findByFactoryIdWithFiltersIncludingPreviousStatus(
                    factoryId, statuses, priorities, categoryId, groupId, pageable);
            }
        } else {
            // 기본 모드에서는 기존 생산계획 로직 사용
            List<PartOrderStatus> planStatuses = Arrays.asList(
                PartOrderStatus.UNDER_REVIEW,
                PartOrderStatus.PLAN_CONFIRMED,
                PartOrderStatus.DELAYED
            );
            log.info("기본 생산계획 상태 사용: {}", planStatuses);

            LocalDateTime cutoffDate = includeRecentDays == -1 ?
                LocalDateTime.of(1900, 1, 1, 0, 0) :
                LocalDateTime.now().minusDays(includeRecentDays);
            log.info("기본 모드 cutoffDate 설정: {}, includeRecentDays: {}", cutoffDate, includeRecentDays);

            if (StringUtils.hasText(query)) {
                String searchQuery = "%" + query.trim() + "%";
                log.info("검색어 포함 생산계획 쿼리 실행: {}", searchQuery);
                partOrderPage = partOrderRepository.findProductionPlansWithFiltersAndSearch(
                    factoryId, planStatuses, priorities, categoryId, groupId, searchQuery, cutoffDate, pageable);
            } else {
                log.info("생산계획 쿼리 실행 - planStatuses: {}, cutoffDate: {}", planStatuses, cutoffDate);
                partOrderPage = partOrderRepository.findProductionPlansWithFilters(
                    factoryId, planStatuses, priorities, categoryId, groupId, cutoffDate, pageable);
            }
        }

        log.info("쿼리 결과 - 총 요소 수: {}, 총 페이지: {}, 현재 페이지 요소 수: {}",
            partOrderPage.getTotalElements(), partOrderPage.getTotalPages(), partOrderPage.getNumberOfElements());

        List<PartOrderResponseDto> content = partOrderPage.getContent().stream()
                .map(partOrder -> {
                    partOrder.calculateProgressByDate();
                    return toProductionPlanResponseDto(partOrder);
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

                    // 카테고리 이름 조회
                    String categoryName = null;
                    if (part.getCategoryId() != null) {
                        categoryName = partCategoryProjectionRepository.findByCategoryId(part.getCategoryId())
                            .map(PartCategoryProjection::getCategoryName)
                            .orElse(null);
                    }

                    // 그룹 이름 조회
                    String groupName = null;
                    if (part.getGroupId() != null) {
                        groupName = partGroupProjectionRepository.findByGroupId(part.getGroupId())
                            .map(PartGroupProjection::getGroupName)
                            .orElse(null);
                    }

                    return PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(part.getPartId())
                        .partName(part.getName())
                        .partCode(part.getCode())
                        .partGroup(part.getGroupId() != null ? String.valueOf(part.getGroupId()) : null)
                        .partCategory(part.getCategoryId() != null ? String.valueOf(part.getCategoryId()) : null)
                        .partGroupName(groupName)
                        .partCategoryName(categoryName)
                        .quantity(item.getQuantity())
                        .build();
                })
                .toList();

        // FactoryProjection에서 공장 정보 조회
        FactoryProjection factory = factoryProjectionRepository.findById(partOrder.getFactoryId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .orderCode(partOrder.getOrderCode())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(partOrder.getStatus().name())
                .factoryId(factory.getBranchId())
                .factoryName(factory.getBranchName())
                .externalPartOrderId(partOrder.getExternalPartOrderId())
                .requiredDate(partOrder.getRequiredDate())
                .scheduledDate(partOrder.getScheduledDate())
                .minimumStartDate(partOrder.getMinimumStartDate())
                .progressRate(Math.round(partOrder.getProgressRate() * 100.0) / 100.0)
                .rejectionReason(partOrder.getRejectionReason())
                .dDay(partOrder.getDDay())
                .priority(partOrder.getPriority() != null ? partOrder.getPriority().name() : null)
                .materialAvailability(partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null)
                .orderType(partOrder.getOrderType() != null ? partOrder.getOrderType().name() : null)
                .items(itemDtos)
                .build();
    }

    // 생산계획용 DTO 변환 메서드 (IN_PROGRESS인 경우 이전 상태로 표시)
    private PartOrderResponseDto toProductionPlanResponseDto(PartOrder partOrder) {
        List<PartOrderResponseDto.PartOrderItemDto> itemDtos = partOrder.getItems().stream()
                .map(item -> {
                    PartProjection part = partProjectionRepository.findByPartId(item.getPartId())
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));

                    // 카테고리 이름 조회
                    String categoryName = null;
                    if (part.getCategoryId() != null) {
                        categoryName = partCategoryProjectionRepository.findByCategoryId(part.getCategoryId())
                            .map(PartCategoryProjection::getCategoryName)
                            .orElse(null);
                    }

                    // 그룹 이름 조회
                    String groupName = null;
                    if (part.getGroupId() != null) {
                        groupName = partGroupProjectionRepository.findByGroupId(part.getGroupId())
                            .map(PartGroupProjection::getGroupName)
                            .orElse(null);
                    }

                    return PartOrderResponseDto.PartOrderItemDto.builder()
                        .partId(part.getPartId())
                        .partName(part.getName())
                        .partCode(part.getCode())
                        .partGroup(part.getGroupId() != null ? String.valueOf(part.getGroupId()) : null)
                        .partCategory(part.getCategoryId() != null ? String.valueOf(part.getCategoryId()) : null)
                        .partGroupName(groupName)
                        .partCategoryName(categoryName)
                        .quantity(item.getQuantity())
                        .build();
                })
                .toList();

        // FactoryProjection에서 공장 정보 조회
        FactoryProjection factory = factoryProjectionRepository.findById(partOrder.getFactoryId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        // 생산계획에서는 IN_PROGRESS인 경우 이전 상태로 표시
        String displayStatus = partOrder.getStatus().name();
        if (partOrder.getStatus() == PartOrderStatus.IN_PROGRESS && partOrder.getPreviousStatus() != null) {
            displayStatus = partOrder.getPreviousStatus().name();
        }

        return PartOrderResponseDto.builder()
                .orderId(partOrder.getId())
                .orderCode(partOrder.getOrderCode())
                .warehouseName(partOrder.getWarehouseName())
                .orderDate(partOrder.getOrderDate())
                .status(displayStatus)
                .factoryId(factory.getBranchId())
                .factoryName(factory.getBranchName())
                 .externalPartOrderId(partOrder.getExternalPartOrderId())
                .requiredDate(partOrder.getRequiredDate())
                .scheduledDate(partOrder.getScheduledDate())
                .minimumStartDate(partOrder.getMinimumStartDate())
                .progressRate(Math.round(partOrder.getProgressRate() * 100.0) / 100.0)
                .rejectionReason(partOrder.getRejectionReason())
                .dDay(partOrder.getDDay())
                .priority(partOrder.getPriority() != null ? partOrder.getPriority().name() : null)
                .materialAvailability(partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null)
                .orderType(partOrder.getOrderType() != null ? partOrder.getOrderType().name() : null)
                .items(itemDtos)
                .build();
    }

    private FactoryProjection findOptimalFactory(Map<Long, Long> requiredMaterials, Long warehouseId, String warehouseName) {
        List<FactoryProjection> factories = factoryProjectionRepository.findAll();

        FactoryProjection optimalFactory = null;
        double bestScore = -1;

        for (FactoryProjection factory : factories) {
            double score = 0;

            // 자재 재고량 확인
            for (Map.Entry<Long, Long> entry : requiredMaterials.entrySet()) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                        .findFirstByFactoryIdAndMaterialId(factory.getBranchId(), entry.getKey())
                        .orElse(null);

                if (factoryMaterial != null && factoryMaterial.getQuantity() >= entry.getValue()) {
                    score += 10; // 자재 충분 시 점수 추가
                } else {
                    score -= 5; // 자재 부족 시 점수 차감
                }
            }

            // 창고-공장 간 거리 점수 (거리가 가까울수록 높은 점수)
            // 우선 창고 ID로 거리 조회 시도
            BranchFactoryDistance distance = null;
            if (warehouseId != null) {
                distance = branchFactoryDistanceRepository
                        .findByBranchIdAndFactoryId(warehouseId, factory.getBranchId())
                        .orElse(null);
            }

            // 창고 ID로 찾지 못한 경우 창고명으로 조회 (하위 호환성)
            if (distance == null && warehouseName != null) {
                distance = branchFactoryDistanceRepository
                        .findByBranchNameAndFactoryId(warehouseName, factory.getBranchId())
                        .orElse(null);
            }

            if (distance != null) {
                double distanceKm = distance.getDistanceKm();
                if (distanceKm <= 100) {
                    score += 30;
                } else if (distanceKm <= 200) {
                    score += 20;
                } else if (distanceKm <= 300) {
                    score += 10;
                } else {
                    score += 5; // 멀어도 최소 점수
                }

                log.info("공장 {} - 거리: {}km, 거리 점수: {}, 조회방법: {}",
                    factory.getBranchName(), distanceKm,
                    distanceKm <= 100 ? 30 : distanceKm <= 200 ? 20 : distanceKm <= 300 ? 10 : 5,
                    warehouseId != null ? "창고ID" : "창고명");
            } else {
                // 거리 정보가 없는 경우 기본 위치 근접성 체크 (하위 호환성)
                if (warehouseName != null && factory.getAddress() != null && factory.getAddress().contains(warehouseName)) {
                    score += 15; // 위치 근접성 보너스 (거리 정보��다 낮은 점수)
                }
                log.info("공장 {} - 거리 정보 없음, 주소 기반 점수 적용", factory.getBranchName());
            }

            log.info("공장 평가 - 이름: {}, 총 점수: {}", factory.getBranchName(), score);

            if (score > bestScore) {
                bestScore = score;
                optimalFactory = factory;
            }
        }

        if (optimalFactory == null) {
            // 적절한 공장이 없으면 첫 번째 공장 선택
            optimalFactory = factories.get(0);
            log.warn("최적 공장을 찾지 못해 첫 번째 공장 선택: {}", optimalFactory.getBranchName());
        } else {
            log.info("최적 공장 선택 완료 - 공장명: {}, 최종 점수: {}", optimalFactory.getBranchName(), bestScore);
        }

        return optimalFactory;
    }

    // 자재 차감 projection 기반으로 변경
    private void deductMaterials(PartOrder partOrder) {
        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());
            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.MATERIAL_NOT_FOUND));
                double required = bomMaterial.getQuantity() * item.getQuantity(); // Double 값 직접 사용
                factoryMaterial.decreaseQuantity(required);
            }
        }
    }

    // 자재 구매요청 처리 (자재 부족 시 호출) - 종류별 단건 요청으로 변경
    private void requestMaterialPurchase(PartOrder partOrder) {
        log.info("자재 구매요청 처리 시작 - 주문 ID: {}", partOrder.getId());

        // 자재별로 부족량을 집계
        Map<Long, MaterialPurchaseInfo> materialRequirements = new HashMap<>();

        for (PartOrderItem item : partOrder.getItems()) {
            BomProjection bomProjection = bomProjectionRepository.findByPartId(item.getPartId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.BOM_NOT_FOUND));
            List<BomMaterialProjection> materials = bomMaterialProjectionRepository.findByBomId(bomProjection.getBomId());

            for (BomMaterialProjection bomMaterial : materials) {
                FactoryMaterial factoryMaterial = factoryMaterialRepository
                    .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                    .orElse(null);

                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환
                long currentStock = factoryMaterial != null ? Math.round(factoryMaterial.getQuantity()) : 0; // Double을 long으로 변환

                if (currentStock < required) {
                    long shortageAmount = required - currentStock;
                    
                    // 이미 집계된 자재가 있으면 부족량을 추가
                    materialRequirements.merge(bomMaterial.getMaterialId(), 
                        new MaterialPurchaseInfo(bomMaterial.getMaterialId(), shortageAmount),
                        (existing, newInfo) -> new MaterialPurchaseInfo(
                            existing.getMaterialId(), 
                            existing.getShortageAmount() + newInfo.getShortageAmount())
                    );
                }
            }
        }

        // 공장 정보 조회
        FactoryProjection factory = factoryProjectionRepository.findById(partOrder.getFactoryId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        // 자재별로 개별 구매요청 생성 및 전송
        int successCount = 0;
        int failCount = 0;
        List<String> failedMaterials = new ArrayList<>();
        
        for (MaterialPurchaseInfo materialInfo : materialRequirements.values()) {
            try {
                var materialProjection = materialProjectionRepository.findByMaterialId(materialInfo.getMaterialId());

                String materialCode = materialProjection.map(mp -> mp.getCode()).orElse("MTL-" + materialInfo.getMaterialId());
                String materialName = materialProjection.map(mp -> mp.getName()).orElse("UNKNOWN");
                String unit = materialProjection.map(mp -> mp.getMaterialUnit()).orElse("EA");
                Long unitPrice = materialProjection.map(mp -> mp.getStandardCost()).orElse(1000L);
                Integer leadTimeDays = materialProjection.map(mp -> mp.getLeadTime()).orElse(7);

                // standardQuantity를 고려한 주문 수량 계산
                Integer standardQuantity = materialProjection.map(mp -> mp.getStandardQuantity()).orElse(1);
                long actualOrderQuantity = calculateOrderQuantityWithStandardUnit(materialInfo.getShortageAmount(), standardQuantity);

                log.info("자재 주문 수량 계산 - 자재코드: {}, 부족량: {}, 표준수량: {}, 실제주문량: {}",
                    materialCode, materialInfo.getShortageAmount(), standardQuantity, actualOrderQuantity);

                List<PurchaseRequestDto.PurchaseItemDto> singleMaterialItem = List.of(
                    PurchaseRequestDto.PurchaseItemDto.builder()
                            .materialCode(materialCode)
                            .materialName(materialName)
                            .unit(unit)
                            .quantity(actualOrderQuantity)  // 계산된 배수 수량으로 변경
                            .unitPrice(unitPrice)
                            .leadTimeDays(leadTimeDays)
                            .standardQuantity(standardQuantity)  // standardQuantity 추가
                            .build()
                );

                PurchaseRequestDto purchaseRequest = PurchaseRequestDto.builder()
                        .factoryId(factory.getBranchId())
                        .factoryName(factory.getBranchName())
                        .requiredAt(partOrder.getRequiredDate())
                        .requesterName("MRP 시스템")
                        .items(singleMaterialItem)
                        .build();

                purchaseRequestService.sendPurchaseRequest(purchaseRequest);
                
                log.info("자재 개별 구매요청 성공 - 주문 ID: {}, 자재코드: {}, 자재명: {}, 부족수량: {}, 주문수량: {}",
                    partOrder.getId(), materialCode, materialName, materialInfo.getShortageAmount(), actualOrderQuantity);

                successCount++;
                
            } catch (Exception e) {
                String materialName = materialProjectionRepository.findByMaterialId(materialInfo.getMaterialId())
                    .map(mp -> mp.getName())
                    .orElse("UNKNOWN");
                
                failedMaterials.add(materialName);
                
                log.error("자재 개별 구매요청 실패 - 주문 ID: {}, 자재 ID: {}, 자재명: {}, 오류: {}",
                    partOrder.getId(), materialInfo.getMaterialId(), materialName, e.getMessage());
                failCount++;
            }
        }

        if (materialRequirements.isEmpty()) {
            log.info("구매요청할 자재가 없습니다 - 주문 ID: {}", partOrder.getId());
        } else {
            log.info("자재 구매요청 처리 완료 - 주문 ID: {}, 성공: {}, 실패: {}, 총 자재 종류: {}", 
                partOrder.getId(), successCount, failCount, materialRequirements.size());
        }

        // 하나라도 실패한 경우 상세한 예외 발생
        if (failCount > 0) {
            String errorMessage = String.format(
                "자재 구매요청이 실패했습니다. 성공: %d, 실패: %d. 실패한 자재: %s",
                successCount, failCount, String.join(", ", failedMaterials)
            );
            throw new RuntimeException(errorMessage);
        }
    }

    // 표준 수량 단위로 배수 계산하는 메서드 추가
    private long calculateOrderQuantityWithStandardUnit(long shortageAmount, Integer standardQuantity) {
        if (standardQuantity == null || standardQuantity <= 0) {
            return shortageAmount; // 표준수량이 없으면 부족량 그대로 반환
        }

        // 부족량을 표준수량으로 나눈 후 올림하여 배수로 계산
        long multiplier = (long) Math.ceil((double) shortageAmount / standardQuantity);
        long orderQuantity = multiplier * standardQuantity;

        log.debug("배수 계산 - 부족량: {}, 표준수량: {}, 배수: {}, 주문량: {}",
            shortageAmount, standardQuantity, multiplier, orderQuantity);

        return orderQuantity;
    }

    // 부품 주문 생성 (아이템별 단건 주문 생성)
    @Transactional
    public List<PartOrderResponseDto> createPartOrdersSeparately(PartOrderRequestDto request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST);
        }

        log.info("부품 주문 단건 생성 시작 - 아이템 수: {}, 창고ID: {}, 창고명: {}, 외부주문ID: {}",
            request.getItems().size(), request.getWarehouseId(), request.getWarehouseName(), request.getExternalPartOrderId());

        List<PartOrderResponseDto> results = new ArrayList<>();

        // 각 아이템별로 개별 주문 생성
        for (PartOrderRequestDto.PartOrderItemRequestDto item : request.getItems()) {
            try {
                // 필수 필드 검증
                Long partId = item.getPartId();
                Long quantity = item.getQuantity();

                if (partId == null || quantity == null) {
                    log.warn("부족한 아이템 정보 - partId: {}, quantity: {}", partId, quantity);
                    continue;
                }

                // 단일 아이템으로 요청 DTO 생성
                PartOrderRequestDto.PartOrderItemRequestDto singleItem = PartOrderRequestDto.PartOrderItemRequestDto.builder()
                        .partId(partId)
                        .quantity(quantity)
                        .build();

                PartOrderRequestDto singleItemRequest = PartOrderRequestDto.builder()
                        .warehouseId(request.getWarehouseId())
                        .warehouseName(request.getWarehouseName())
                        .requiredDate(request.getRequiredDate())
                        .externalPartOrderId(request.getExternalPartOrderId())
                        .items(List.of(singleItem))
                        .build();

                // 개별 주문 생성
                PartOrderResponseDto orderResult = createPartOrder(singleItemRequest);
                results.add(orderResult);

                log.info("개별 부품 주문 생성 완료 - 주문 ID: {}, 부품 ID: {}, 수량: {}, 외부주문ID: {}",
                    orderResult.getOrderId(), partId, quantity, request.getExternalPartOrderId());

            } catch (Exception e) {
                log.error("개별 부품 주문 생성 실패 - 부품 ID: {}, 수량: {}, 오류: {}",
                    item.getPartId(), item.getQuantity(), e.getMessage(), e);
                // 개별 주문 실패 시에도 다른 주문은 계속 처리
                // 하지만 실패한 아이템에 대한 정보는 로그로 남김
            }
        }

        log.info("부품 주문 단건 생성 완료 - 총 성공: {}/{}개", results.size(), request.getItems().size());
        return results;
    }

//    // 생산지시 API (계획확정 상태에서 진행중으로 변경) - 동시성 제어 추가
//    @Transactional
//    public PartOrderResponseDto startProduction(Long factoryId, Long orderId) {
//        // 비관적 락을 사용하여 동시성 제어
//        PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));
//
//        // PLAN_CONFIRMED 또는 DELAYED 상태에서 생산지시 가능
//        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED &&
//            partOrder.getStatus() != PartOrderStatus.DELAYED) {
//            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
//        }
//
//        // 이미 진행중인 상태인 경우 중복 처리 방지
//        if (partOrder.getStatus() == PartOrderStatus.IN_PROGRESS) {
//            log.warn("주문이 이미 진행중 상태입니다 - 주문 ID: {}", partOrder.getId());
//            return toResponseDto(partOrder);
//        }
//
//        // 진행중 상태로 변경
//        partOrder.startProgress();
//
//        // 자재 차감 (자재가 충분한 경우에만)
//        if (partOrder.getMaterialAvailability() == MaterialAvailability.SUFFICIENT) {
//            deductMaterials(partOrder);
//            log.info("생산지시 완료 및 자재 차감 - 주문 ID: {}", partOrder.getId());
//        } else {
//            log.info("자재 부족으로 자재 차감 없이 생산지시만 완료 - 주문 ID: {}", partOrder.getId());
//        }
//
//        partOrderRepository.save(partOrder);
//
//        // 생산지시로 상태가 변경된 경우 이벤트 발행
//        partOrderEventService.recordPartOrderStatusChanged(partOrder);
//
//        return toResponseDto(partOrder);
//    }

    // MRP 결과 적용 API (클라이언트의 "결과적용" 버튼) - 동시성 제어 추가
    @Transactional
    public PartOrderResponseDto applyMRPResult(Long factoryId, Long orderId) {
        // 비관적 락을 사용하여 동시성 제어
        PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // PLAN_CONFIRMED 또는 DELAYED 상태에서만 결과 적용 가능
        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED &&
            partOrder.getStatus() != PartOrderStatus.DELAYED) {
            throw new BadRequestException(ErrorStatus.INVALID_ORDER_STATUS);
        }

        // 이미 진행중인 상태로 변경된 경우 중복 처리 방지
        if (partOrder.getStatus() == PartOrderStatus.IN_PROGRESS) {
            log.warn("주문이 이미 진행중 상태입니다 - 주문 ID: {}", partOrder.getId());
            return toResponseDto(partOrder);
        }

        if (partOrder.getMaterialAvailability() == MaterialAvailability.INSUFFICIENT) {
            // 자재 부족 시: 자재 구매요청 + 생산지시
            log.info("자재 부족으로 구매요�� 및 생산지시 동시 진행 - 주문 ID: {}", partOrder.getId());

            try {
                // 1. 먼저 자재 구매요청 처리 (실패 가능한 작업을 먼저 수행)
                requestMaterialPurchase(partOrder);

                // 2. 구매요청이 성공한 경우에만 상태 변경
                partOrder.startProgress();
                log.info("자재 구매요청과 함께 생산지시 완료 - 주문 ID: {}", partOrder.getId());

            } catch (Exception e) {
                log.error("자재 구매요청 실패로 인한 MRP 결과 적용 실패 - 주문 ID: {}, 오류: {}",
                    partOrder.getId(), e.getMessage(), e);
                // 구매요청 실패 시 전체 트랜잭션 롤백을 위해 예외 재발생
                throw new BadRequestException(ErrorStatus.EXTERNAL_API_ERROR);
            }

        } else {
            // 자재 충분 시: 생산지시만 (외부 API 호출 없음)
            log.info("자재 충��으로 생산지시만 진행 - 주문 ID: {}", partOrder.getId());

            // 생산지시 처리
            partOrder.startProgress();
            // 자재 차감
            deductMaterials(partOrder);
            log.info("생산지시 완료 및 자재 차감 - 주문 ID: {}", partOrder.getId());
        }

        partOrderRepository.save(partOrder);

        // MRP 결과 적용으로 상태가 변경된 경우 이벤트 발행
        partOrderEventService.recordPartOrderStatusChanged(partOrder);

        return toResponseDto(partOrder);
    }

    // 일괄 MRP 결과 적용 API
    @Transactional
    public List<PartOrderResponseDto> applyBatchMRPResult(Long factoryId, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST);
        }

        log.info("일괄 MRP 결과 적용 시작 - 공장 ID: {}, 주문 수: {}", factoryId, orderIds.size());

        List<PartOrderResponseDto> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long orderId : orderIds) {
            try {
                // 각 주문에 대해 개별적으로 MRP 결과 적용
                PartOrder partOrder = partOrderRepository.findByIdAndFactoryIdWithLock(orderId, factoryId)
                        .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

                // 상태 검증
                if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED &&
                    partOrder.getStatus() != PartOrderStatus.DELAYED) {
                    log.warn("MRP 결과 적용 불가 - 주문 ID: {}, 현재 상태: {}", orderId, partOrder.getStatus());
                    failCount++;
                    continue;
                }

                // 이미 진행중인 상태인 경우 스킵
                if (partOrder.getStatus() == PartOrderStatus.IN_PROGRESS) {
                    log.warn("주문이 이미 진행중 상태 - 주문 ID: {}", orderId);
                    results.add(toResponseDto(partOrder));
                    successCount++;
                    continue;
                }

                // MRP 결과 적용 로직
                if (partOrder.getMaterialAvailability() == MaterialAvailability.INSUFFICIENT) {
                    // 자재 부족 시: 자재 구매요청 + 생산지시
                    try {
                        requestMaterialPurchase(partOrder);
                        partOrder.startProgress();
                        log.info("자재 구매요청과 함께 생산지시 완료 - 주문 ID: {}", orderId);
                    } catch (Exception e) {
                        log.error("자재 구매요청 실패 - 주문 ID: {}, 오류: {}", orderId, e.getMessage());
                        failCount++;
                        continue;
                    }
                } else {
                    // 자재 충분 시: 생산지시만
                    partOrder.startProgress();
                    deductMaterials(partOrder);
                    log.info("생산지시 완료 및 자재 차감 - 주문 ID: {}", orderId);
                }

                partOrderRepository.save(partOrder);

                // MRP 결과 적용으로 상태가 변경된 경우 이벤트 발행
                partOrderEventService.recordPartOrderStatusChanged(partOrder);

                results.add(toResponseDto(partOrder));
                successCount++;

            } catch (Exception e) {
                log.error("MRP 결과 적용 실패 - 주문 ID: {}, 오류: {}", orderId, e.getMessage(), e);
                failCount++;
                // 개별 실패는 전체 작업을 중단하지 않음
            }
        }

        log.info("일괄 MRP 결과 적용 완료 - 공장 ID: {}, 성공: {}, 실패: {}", factoryId, successCount, failCount);
        return results;
    }
    @Transactional
    public PartOrderResponseDto createMpsPartOrder(
            Long factoryId,
            Long warehouseId,
            Long partId,
            Long quantity,
            LocalDateTime requiredDate,
            Long mpsPlanId,
            String mpsOrderCode) {

        log.info("MPS PartOrder 생성 시작 - 공장ID: {}, 부품ID: {}, 수량: {}, 요구일: {}, MpsPlanID: {}, MPS코드: {}",
                factoryId, partId, quantity, requiredDate, mpsPlanId, mpsOrderCode);

        // 공장 정보 조회
        FactoryProjection factory = factoryProjectionRepository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.FACTORY_NOT_FOUND));

        // 부품 정보 조회 (검증용)
        partProjectionRepository.findByPartId(partId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_NOT_FOUND));

        // MPS 주문 코드 생성
        String orderCode = partOrderCodeGenerator.generateOrderCode();

        // MPS PartOrder 생성 (MPS 전용 상태로 시작)
        PartOrder partOrder = PartOrder.builder()
                .factoryId(factoryId)
                .warehouseId(warehouseId)
                .status(PartOrderStatus.UNDER_REVIEW) // 초기 상태는 검토중
                .warehouseName("MPS-" + factory.getBranchName()) // MPS 식별용 창고명
                .orderDate(LocalDateTime.now())
                .requiredDate(requiredDate)
                .orderCode(orderCode)
                .externalPartOrderId(mpsPlanId) // MpsPlanId를 외부 주문 ID로 사용
                .orderType(PartOrderType.MPS) // MPS 타입으로 설정
                .build();

        // 부품 아이템 추가
        PartOrderItem partOrderItem = PartOrderItem.builder()
                .partOrder(partOrder)
                .partId(partId)
                .quantity(quantity)
                .build();

        partOrder.getItems().add(partOrderItem);

        // 우선순위 계산 및 설정 (MPS는 높은 우선순위)
        partOrder.calculateAndSetPriority();

        // MPS 주문은 높은 우선순위로 강제 설정 (우선순위가 낮으면 직접 변경)
        if (partOrder.getPriority() == PartOrderPriority.LOW ||
                partOrder.getPriority() == PartOrderPriority.MEDIUM) {
            log.info("MPS 주문 우선순위를 HIGH로 강제 설정 - 기존: {}", partOrder.getPriority());
        }

        // 초기 자재가용성 확인
        boolean materialShortage = checkInitialMaterialAvailability(partOrder);
        partOrder.updateMaterialAvailability(
                materialShortage ? MaterialAvailability.INSUFFICIENT : MaterialAvailability.SUFFICIENT
        );

        // 저장
        partOrderRepository.save(partOrder);

        log.info("MPS PartOrder 생성 완료 - 주문ID: {}, 주문코드: {}, 우선순위: {}, 자재가용성: {}, MpsPlanID: {}, MPS코드: {}",
                partOrder.getId(), orderCode, partOrder.getPriority(), partOrder.getMaterialAvailability(), mpsPlanId, mpsOrderCode);

        return toResponseDto(partOrder);
    }


    /**
     * MPS 주문 자동 MRP 결과 적용 (스케줄러에서 호출)
     */
    @Transactional
    public void applyMrpResultsAutomatically(Long orderId) {
        log.info("MPS 주문 자동 MRP 결과 적용 시작 - 주문 ID: {}", orderId);

        // 주문 조회 (락 사용)
        PartOrder partOrder = partOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.PART_ORDER_NOT_FOUND));

        // MPS 타입 검증
        if (partOrder.getOrderType() != PartOrderType.MPS) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST);
        }

        // 계획확정 상태 검증
        if (partOrder.getStatus() != PartOrderStatus.PLAN_CONFIRMED && partOrder.getStatus() != PartOrderStatus.DELAYED) {
            log.warn("MPS 주문이 계획확정 상태가 아닙니다 - 주문 ID: {}, 현재 상태: {}",
                    orderId, partOrder.getStatus());
            return;
        }

        // 시작일 검증
        LocalDateTime now = LocalDateTime.now();
        if (partOrder.getMinimumStartDate() != null && partOrder.getMinimumStartDate().isAfter(now)) {
            log.warn("MPS 주문의 시작일이 아직 도래하지 않았습니다 - 주문 ID: {}, 시작일: {}",
                    orderId, partOrder.getMinimumStartDate());
            return;
        }

        try {
            if (partOrder.getMaterialAvailability() == MaterialAvailability.INSUFFICIENT) {
                // 자재 부족 시: 자재 구매요청 + 생산지시
                log.info("MPS 주문 자재 부족으로 구매요청 및 생산지시 동시 진행 - 주문 ID: {}", orderId);

                // 구매요청 처리
                requestMaterialPurchase(partOrder);

                // 상태 변경 (생산중으로)
                partOrder.startProgress();

                log.info("MPS 주문 자재 구매요청과 함께 생산 시작 완료 - 주문 ID: {}", orderId);

            } else {
                // 자재 충분 시: 생산지시만
                log.info("MPS 주문 자재 충분으로 생산지시만 진행 - 주문 ID: {}", orderId);

                // 생산지시 처리 및 자재 차감
                partOrder.startProgress();
                deductMaterials(partOrder);

                log.info("MPS 주문 생산지시 완료 및 자재 차감 - 주문 ID: {}", orderId);
            }

            partOrderRepository.save(partOrder);

            // 상태 변경 이벤트 발행
            partOrderEventService.recordPartOrderStatusChanged(partOrder);

            log.info("MPS 주문 자동 MRP 결과 적용 성공 - 주문 ID: {}, 새 상태: {}",
                    orderId, partOrder.getStatus());

        } catch (Exception e) {
            log.error("MPS 주문 자동 MRP 결과 적용 실패 - 주문 ID: {}, 오류: {}",
                    orderId, e.getMessage(), e);
            throw e; // 스케줄러에서 오류 로그를 위해 예외 재발생
        }
    }

    // 자재 구매 정보를 담는 내부 클래스
    private static class MaterialPurchaseInfo {
        private final Long materialId;
        private final Long shortageAmount;

        public MaterialPurchaseInfo(Long materialId, Long shortageAmount) {
            this.materialId = materialId;
            this.shortageAmount = shortageAmount;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public Long getShortageAmount() {
            return shortageAmount;
        }
    }
}
