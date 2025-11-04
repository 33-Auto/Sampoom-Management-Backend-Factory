package com.sampoom.factory.api.part.controller;

import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.PartOrderPriority;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.service.PartOrderService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.PageResponseDto;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/{factoryId}/part")
@Tag(name = "PartOrder", description = "PartOrder 관련 API 입니다.")
public class PartOrderController {

    private final PartOrderService partOrderService;

    @Operation(summary = "부품 주문 완료 처리", description = "진행 중인 부품 주문을 완료 상태로 변경합니다.")
    @PatchMapping("/order/{orderId}/complete")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> completePartOrder(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.completePartOrder(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "부품 주문 진행률 업데이트", description = "부품 주문의 진행률과 D-day를 업데이트합니다.")
    @PatchMapping("/order/{orderId}/progress")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> updatePartOrderProgress(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.updatePartOrderProgress(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "MRP 실행", description = "검토중인 부품 주문에 대해 MRP를 실행합니다.")
    @PostMapping("/order/{orderId}/mrp")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> executeMRP(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.executeMRP(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "일괄 MRP 실행", description = "여러 부품 주문에 대해 MRP를 일괄 실행합니다.")
    @PostMapping("/orders/mrp/batch")
    public ResponseEntity<ApiResponse<List<PartOrderResponseDto>>> executeBatchMRP(
            @PathVariable Long factoryId,
            @RequestBody List<Long> orderIds
    ) {
        List<PartOrderResponseDto> response = partOrderService.executeBatchMRP(factoryId, orderIds);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "생산지시", description = "계획확정된 부품 주문을 진행중 상태로 변경합니다.")
    @PostMapping("/order/{orderId}/start-production")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> startProduction(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.startProduction(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "부품 주문 조회", description = "특정 부품 주문의 상세 정보를 조회합니다.")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> getPartOrder(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.getPartOrder(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "부품 주문 목록 조회", description = "공장의 부품 주문 목록을 조회합니다. 여러 상태와 우선순위를 동시에 필터링할 수 있습니다.")
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponseDto<PartOrderResponseDto>>> getPartOrders(
            @PathVariable Long factoryId,
            @RequestParam(required = false) List<PartOrderStatus> statuses,
            @RequestParam(required = false) List<PartOrderPriority> priorities,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponseDto<PartOrderResponseDto> response = partOrderService.getPartOrders(factoryId, statuses, priorities, page, size);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "MRP 결과 적용", description = "MRP 실행 결과를 적용합니다. 자재 부족 시 구매요청과 생산지시를 함께 처리하고, 자재 충분 시 생산지시만 처리합니다.")
    @PostMapping("/order/{orderId}/apply-mrp")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> applyMRPResult(
            @PathVariable Long factoryId,
            @PathVariable Long orderId
    ) {
        PartOrderResponseDto response = partOrderService.applyMRPResult(factoryId, orderId);
        return ApiResponse.success(SuccessStatus.OK, response);
    }

    @Operation(summary = "일괄 MRP 결과 적용", description = "여러 부품 주문에 대해 MRP 결과를 일괄 적용합니다.")
    @PostMapping("/orders/apply-mrp/batch")
    public ResponseEntity<ApiResponse<List<PartOrderResponseDto>>> applyBatchMRPResult(
            @PathVariable Long factoryId,
            @RequestBody List<Long> orderIds
    ) {
        List<PartOrderResponseDto> response = partOrderService.applyBatchMRPResult(factoryId, orderIds);
        return ApiResponse.success(SuccessStatus.OK, response);
    }
}
