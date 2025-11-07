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

    // MRP 실행 API (별도 분리) - 동시성 제어 추가
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
            log.info("자재 충분 - 주문 ID: {}, 생산 리드타임: {}��",
                partOrder.getId(), maxProductionLeadTime);
        }

        partOrderRepository.save(partOrder);

        // MRP 실행으로 상태가 변경된 경우 이벤트 발행
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
                        .findFirstByFactoryIdAndMaterialId(partOrder.getFactoryId(), bomMaterial.getMaterialId())
                        .orElse(null);

                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환

                if (factoryMaterial == null || factoryMaterial.getQuantity() < required) {
                    materialShortage = true;
                    int materialLeadTime = materialProjectionRepository.findByMaterialId(bomMaterial.getMaterialId())
                        .map(mp -> mp.getLeadTime())
                        .orElse(7);
                    maxMaterialLeadTime = Math.max(maxMaterialLeadTime, materialLeadTime);
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

    // 주문 목록 조회 - 여러 상태 필터링 지원
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

    // 주문 목록 조회 - 카테고리, 그룹 필터링 추가 (컨트롤러 시그니처와 정확히 맞춤)
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

    /**
     * 생산계획 목록 조회 (계획 상태 + 최근 IN_PROGRESS로 전환된 데이터 포함) - 컨트롤러 시그니처와 정확히 맞춤
     */
    public PageResponseDto<PartOrderResponseDto> getProductionPlans(Long factoryId, List<PartOrderPriority> priorities,
                                                                  String query, Long categoryId, Long groupId,
                                                                  int page, int size, int includeRecentDays) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // 생산계획 상태: UNDER_REVIEW, PLAN_CONFIRMED, DELAYED
        List<PartOrderStatus> planStatuses = Arrays.asList(
            PartOrderStatus.UNDER_REVIEW,
            PartOrderStatus.PLAN_CONFIRMED,
            PartOrderStatus.DELAYED
        );

        // 최근 IN_PROGRESS로 전환된 데이터를 포함하기 위한 기준일 계산
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(includeRecentDays);

        Page<PartOrder> partOrderPage;

        if (StringUtils.hasText(query)) {
            String searchQuery = "%" + query.trim() + "%";
            partOrderPage = partOrderRepository.findProductionPlansWithFiltersAndSearch(
                factoryId, planStatuses, priorities, categoryId, groupId, searchQuery, cutoffDate, pageable);
        } else {
            partOrderPage = partOrderRepository.findProductionPlansWithFilters(
                factoryId, planStatuses, priorities, categoryId, groupId, cutoffDate, pageable);
        }

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
                .progressRate(partOrder.getProgressRate())
                .rejectionReason(partOrder.getRejectionReason())
                .dDay(partOrder.getDDay())
                .priority(partOrder.getPriority() != null ? partOrder.getPriority().name() : null)
                .materialAvailability(partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null)
                .items(itemDtos)
                .build();
    }

    // 생산계획용 DTO 변환 메서드 (IN_PROGRESS인 경우 이전 상태로 표시)
    private PartOrderResponseDto toProductionPlanResponseDto(PartOrder partOrder) {
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
                .progressRate(partOrder.getProgressRate())
                .rejectionReason(partOrder.getRejectionReason())
                .dDay(partOrder.getDDay())
                .priority(partOrder.getPriority() != null ? partOrder.getPriority().name() : null)
                .materialAvailability(partOrder.getMaterialAvailability() != null ? partOrder.getMaterialAvailability().name() : null)
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
                long required = Math.round(bomMaterial.getQuantity() * item.getQuantity()); // Double에서 long으로 변환
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
                long currentStock = factoryMaterial != null ? factoryMaterial.getQuantity() : 0;

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

                List<PurchaseRequestDto.PurchaseItemDto> singleMaterialItem = List.of(
                    PurchaseRequestDto.PurchaseItemDto.builder()
                            .materialCode(materialCode)
                            .materialName(materialName)
                            .unit(unit)
                            .quantity(materialInfo.getShortageAmount())
                            .unitPrice(unitPrice)
                            .leadTimeDays(leadTimeDays)
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
                
                log.info("자재 개별 구매요청 성공 - 주문 ID: {}, 자재코드: {}, 자재명: {}, 부족수량: {}", 
                    partOrder.getId(), materialCode, materialName, materialInfo.getShortageAmount());
                
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
                // 이벤트에서 온 데이터인 경우 materialId를 partId로 매핑
                Long partId = item.getPartId() != null ? item.getPartId() : item.getMaterialId();
                Long quantity = item.getQuantity() != null ? item.getQuantity() :
                               (item.getRequestQuantity() != null ? item.getRequestQuantity().longValue() : null);

                if (partId == null || quantity == null) {
                    log.warn("부족한 아이템 정보 - partId: {}, materialId: {}, quantity: {}, requestQuantity: {}",
                            item.getPartId(), item.getMaterialId(), item.getQuantity(), item.getRequestQuantity());
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
                    item.getPartId() != null ? item.getPartId() : item.getMaterialId(),
                    item.getQuantity() != null ? item.getQuantity() : item.getRequestQuantity(),
                    e.getMessage(), e);
                // 개별 주문 실패 시에도 다른 주문은 계속 처리
                // 하지만 실패한 아이템에 대한 정보는 로그로 남김
            }
        }

        log.info("부품 주문 단건 생성 완료 - 총 아이템: {}, 성공한 주문: {}, 외부주문ID: {}",
            request.getItems().size(), results.size(), request.getExternalPartOrderId());

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
}
