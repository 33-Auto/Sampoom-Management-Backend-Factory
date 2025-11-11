package com.sampoom.factory.api.mps.controller;

import com.sampoom.factory.api.mps.dto.MpsPlanResponse;
import com.sampoom.factory.api.mps.dto.MpsResponse;
import com.sampoom.factory.api.mps.entity.Mps;
import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.service.MpsService;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "MPS", description = "MPS(Master Production Schedule) 관리 API")
@RestController
@RequestMapping("/{factoryId}/mps")
@RequiredArgsConstructor
@Slf4j
public class MpsController {

    private final MpsService mpsService;

    @Operation(summary = "MPS 상세 조회", description = "부품ID, 예측달, 창고ID로 MPS를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MpsResponse>> getMps(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId,
            @Parameter(description = "부품 ID", required = true) @RequestParam Long partId,
            @Parameter(description = "예측 달 (YYYY-MM-DD 형식)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forecastMonth,
            @Parameter(description = "창고 ID", required = true) @RequestParam Long warehouseId) {

        log.info("MPS 조회 요청 - factoryId: {}, partId: {}, forecastMonth: {}, warehouseId: {}",
                factoryId, partId, forecastMonth, warehouseId);

        Mps mps = mpsService.getMpsByFactoryAndPartAndForecastAndWarehouse(factoryId, partId, forecastMonth, warehouseId);
        MpsResponse response = MpsResponse.from(mps);

        log.info("MPS 조회 성공 - mpsId: {}", response.getMpsId());

        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "MPS 실행", description = "MPS를 실행하여 상세 생산 계획(MpsPlan)을 생성합니다.")
    @PostMapping("/{mpsId}/execute")
    public ResponseEntity<ApiResponse<List<MpsPlanResponse>>> executeMps(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId,
            @Parameter(description = "MPS ID") @PathVariable Long mpsId) {
        log.info("MPS 실행 요청 - factoryId: {}, mpsId: {}", factoryId, mpsId);

        List<MpsPlan> mpsPlans = mpsService.executeMps(mpsId);
        List<MpsPlanResponse> responses = mpsPlans.stream()
                .map(plan -> MpsPlanResponse.builder()
                        .mpsPlanId(plan.getMpsPlanId())
                        .mpsId(plan.getMps().getMpsId())
                        .cycleNumber(plan.getCycleNumber())
                        .requiredDate(plan.getRequiredDate())
                        .productionQuantity(plan.getProductionQuantity())
                        .remainingTotalProduction(plan.getRemainingTotalProduction())
                        .status(plan.getStatus())
                        .createdAt(plan.getCreatedAt())
                        .updatedAt(plan.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(SuccessStatus.CREATED, responses);
    }

    @Operation(summary = "MPS 계획 목록 조회", description = "MPS ID로 해당하는 MpsPlan 목록을 조회합니다.")
    @GetMapping("/{mpsId}/plans")
    public ResponseEntity<ApiResponse<List<MpsPlanResponse>>> getMpsPlans(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId,
            @Parameter(description = "MPS ID", required = true) @PathVariable Long mpsId) {

        log.info("MPS 계획 목록 조회 요청 - factoryId: {}, mpsId: {}", factoryId, mpsId);

        List<MpsPlan> mpsPlans = mpsService.getMpsPlansByMpsId(mpsId);
        List<MpsPlanResponse> responses = mpsPlans.stream()
                .map(plan -> MpsPlanResponse.builder()
                        .mpsPlanId(plan.getMpsPlanId())
                        .mpsId(plan.getMps().getMpsId())
                        .cycleNumber(plan.getCycleNumber())
                        .requiredDate(plan.getRequiredDate())
                        .productionQuantity(plan.getProductionQuantity())
                        .remainingTotalProduction(plan.getRemainingTotalProduction())
                        .status(plan.getStatus())
                        .createdAt(plan.getCreatedAt())
                        .updatedAt(plan.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("MPS 계획 목록 조회 성공 - factoryId: {}, mpsId: {}, 계획 수: {}",
                factoryId, mpsId, responses.size());

        return ApiResponse.success(SuccessStatus.OK, responses);
    }

    @Operation(summary = "MPS 확정", description = "MPS를 확정하여 각 MpsPlan을 기반으로 PartOrder를 생성합니다.")
    @PostMapping("/{mpsId}/confirm")
    public ResponseEntity<ApiResponse<List<PartOrderResponseDto>>> confirmMps(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId,
            @Parameter(description = "MPS ID", required = true) @PathVariable Long mpsId) {

        log.info("MPS 확정 요청 - factoryId: {}, mpsId: {}", factoryId, mpsId);

        List<PartOrderResponseDto> partOrders = mpsService.confirmMps(factoryId, mpsId);

        log.info("MPS 확정 성공 - factoryId: {}, mpsId: {}, 생성된 PartOrder 수: {}",
                factoryId, mpsId, partOrders.size());

        return ApiResponse.success(SuccessStatus.CREATED, partOrders);
    }

    @Operation(summary = "MPS 부품 목록 조회", description = "해당 공장에 저장된 MPS의 모든 부품 ID 목록을 조회합니다.")
    @GetMapping("/parts")
    public ResponseEntity<ApiResponse<List<Long>>> getMpsPartList(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId) {

        log.info("MPS 부품 목록 조회 요청 - factoryId: {}", factoryId);

        List<Long> partIds = mpsService.getMpsPartListByFactory(factoryId);

        log.info("MPS 부품 목록 조회 성공 - factoryId: {}, 부품 수: {}", factoryId, partIds.size());

        return ApiResponse.success(SuccessStatus.OK, partIds);
    }

    @Operation(summary = "특정 부품의 예측 달 조회", description = "특정 부품 ID에 대한 모든 예측 달(targetDate) 목록을 조회합니다.")
    @GetMapping("/parts/{partId}/forecast-months")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getPartForecastMonths(
            @Parameter(description = "공장 ID", required = true) @PathVariable Long factoryId,
            @Parameter(description = "부품 ID", required = true) @PathVariable Long partId) {

        log.info("부품 예측 달 조회 요청 - factoryId: {}, partId: {}", factoryId, partId);

        List<LocalDate> forecastMonths = mpsService.getForecastMonthsByFactoryAndPart(factoryId, partId);

        log.info("부품 예측 달 조회 성공 - factoryId: {}, partId: {}, 예측 달 수: {}",
                factoryId, partId, forecastMonths.size());

        return ApiResponse.success(SuccessStatus.OK, forecastMonths);
    }
}
