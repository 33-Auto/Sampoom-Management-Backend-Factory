package com.sampoom.factory.api.part.controller;


import com.sampoom.factory.api.part.dto.MpsOrderInfoDto;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.service.PartOrderSchedulerService;
import com.sampoom.factory.api.part.service.PartOrderService;
import com.sampoom.factory.api.part.service.MpsTestService;
import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "PartOrder", description = "PartOrder 관련 API 입니다.")
public class PartOrderApiController {

    private final PartOrderService partOrderService;
    private final PartOrderSchedulerService partOrderSchedulerService;
    private final MpsTestService mpsTestService;

    @Operation(summary = "부품 주문 생성", description = "적절한 공장을 자동으로 선택하여 부품 주문을 생성합니다. 여러 아이템이 있는 경우 각각 단건으로 나누어 개별 주문을 생성합니다.")
    @PostMapping("/part/order")
    public ResponseEntity<ApiResponse<List<PartOrderResponseDto>>> createPartOrder(
            @RequestBody PartOrderRequestDto request
    ) {
        List<PartOrderResponseDto> response = partOrderService.createPartOrdersSeparately(request);
        return ApiResponse.success(SuccessStatus.CREATED, response);
    }

    @Operation(summary = "[테스트용] MPS 주문 자동 MRP 결과 적용 스케줄러 수동 실행",
            description = "MPS 주문의 자동 MRP 결과 적용 스케줄러를 수동으로 실행합니다. 테스트 목적으로 사용됩니다.")
    @PostMapping("/part/order/test/mps-auto-apply")
    public ResponseEntity<ApiResponse<String>> testMpsAutoApply() {
        partOrderSchedulerService.autoApplyMrpForMpsOrders();
        return ApiResponse.success(SuccessStatus.OK, "MPS 주문 자동 MRP 결과 적용 스케줄러가 수동으로 실행되었습니다.");
    }

    @Operation(summary = "[테스트용] 특정 MPS 주문 자동 MRP 결과 적용",
            description = "특정 MPS 주문에 대해 자동 MRP 결과 적용을 수동으로 실행합니다.")
    @PostMapping("/part/order/{orderId}/test/auto-apply")
    public ResponseEntity<ApiResponse<String>> testMpsOrderAutoApply(@PathVariable Long orderId) {
        partOrderService.applyMrpResultsAutomatically(orderId);
        return ApiResponse.success(SuccessStatus.OK, "MPS 주문 " + orderId + "에 대한 자동 MRP 결과 적용이 완료되었습니다.");
    }

    @Operation(summary = "[테스트용] MPS 주문 생성 및 테스트 준비",
            description = "테스트용 MPS 주문을 생성하고 MRP 실행 후 시작일을 현재 시간으로 설정합니다.")
    @PostMapping("/part/order/test/create-mps")
    public ResponseEntity<ApiResponse<Long>> createTestMpsOrder(
            @RequestParam Long factoryId,
            @RequestParam Long partId,
            @RequestParam(defaultValue = "100") Long quantity) {
        Long orderId = mpsTestService.createAndPrepareMpsOrderForTest(factoryId, partId, quantity);
        return ApiResponse.success(SuccessStatus.CREATED, orderId);
    }

    @Operation(summary = "[테스트용] MPS 주문 시작일을 현재 시간으로 설정",
            description = "MPS 주문의 최소 시작일을 현재 시간으로 설정하여 즉시 자동 처리 대상이 되도록 합니다.")
    @PostMapping("/part/order/{orderId}/test/set-start-now")
    public ResponseEntity<ApiResponse<String>> setMpsStartDateToNow(@PathVariable Long orderId) {
        mpsTestService.setMpsOrderStartDateToNow(orderId);
        return ApiResponse.success(SuccessStatus.OK, "MPS 주문 " + orderId + "의 시작일이 현재 시간으로 설정되었습니다.");
    }

    @Operation(summary = "[테스트용] 자동 처리 대상 MPS 주문 조회",
            description = "현재 자동 처리 대상인 MPS 주문들을 조회합니다.")
    @GetMapping("/part/order/test/mps-ready")
    public ResponseEntity<ApiResponse<List<MpsOrderInfoDto>>> getMpsOrdersReadyForProcessing() {
        List<MpsOrderInfoDto> orders = mpsTestService.findMpsOrdersReadyForAutoProcessing();
        return ApiResponse.success(SuccessStatus.OK, orders);
    }

    @Operation(summary = "[테스트용] MPS 주문 정보 출력",
            description = "MPS 주문의 상세 정보를 로그로 출력합니다.")
    @PostMapping("/part/order/{orderId}/test/print-info")
    public ResponseEntity<ApiResponse<String>> printMpsOrderInfo(@PathVariable Long orderId) {
        mpsTestService.printMpsOrderInfo(orderId);
        return ApiResponse.success(SuccessStatus.OK, "MPS 주문 " + orderId + " 정보가 로그에 출력되었습니다.");
    }
}
