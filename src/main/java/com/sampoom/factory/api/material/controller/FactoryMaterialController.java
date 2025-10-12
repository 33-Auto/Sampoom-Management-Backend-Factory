package com.sampoom.factory.api.material.controller;

import com.sampoom.factory.api.material.dto.MaterialCategoryResponseDto;
import com.sampoom.factory.api.material.dto.MaterialResponseDto;
import com.sampoom.factory.common.response.PageResponseDto;
import com.sampoom.factory.api.material.service.FactoryMaterialService;
import com.sampoom.factory.api.material.dto.MaterialOrderRequestDto;
import com.sampoom.factory.api.material.dto.MaterialOrderResponseDto;
import com.sampoom.factory.api.material.service.MaterialOrderService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FactoryMaterial", description = "FactoryMaterial 관련 API 입니다.")
@RestController
@RequestMapping("/api/factory")
@RequiredArgsConstructor
public class FactoryMaterialController {

    private final FactoryMaterialService factoryMaterialService;
    private final MaterialOrderService materialOrderService;

    @Operation(summary = "자재 카테고리 조회", description = "모든 자재 카테고리를 조회합니다.")
    @GetMapping("/material/categories")
    public ResponseEntity<ApiResponse<List<MaterialCategoryResponseDto>>> getMaterialCategories() {
        return ApiResponse.success(SuccessStatus.OK, factoryMaterialService.getAllMaterialCategories());
    }

    @Operation(summary = "공장별 자재 카테고리별 자재 조회", description = "특정 공장의 특정 카테고리에 속한 자재를 조회합니다.")
    @GetMapping("/{factoryId}/material/category/{categoryId}")
    public ResponseEntity<ApiResponse<PageResponseDto<MaterialResponseDto>>> getMaterialsByFactoryAndCategory(
            @PathVariable Long factoryId,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK,
                factoryMaterialService.getMaterialsByFactoryAndCategory(factoryId, categoryId, page, size));
    }

    @Operation(summary = "공장별 자재 목록 조회", description = "특정 공장에 있는 모든 자재를 조회합니다.")
    @GetMapping("/{factoryId}/material")
    public ResponseEntity<ApiResponse<PageResponseDto<MaterialResponseDto>>> getMaterialsByFactory(
            @PathVariable Long factoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK,
                factoryMaterialService.getMaterialsByFactoryId(factoryId, page, size));
    }

    @Operation(summary = "자재 주문 생성", description = "공장에 필요한 자재 주문을 생성합니다.")
    @PostMapping("/{factoryId}/material/order")
    public ResponseEntity<ApiResponse<MaterialOrderResponseDto>> createMaterialOrder(
            @PathVariable Long factoryId,
            @RequestBody MaterialOrderRequestDto requestDto) {
        return ApiResponse.success(SuccessStatus.CREATED,
                materialOrderService.createMaterialOrder(factoryId, requestDto));
    }

    @Operation(summary = "자재 주문 목록 조회", description = "공장의 자재 주문 목록을 조회합니다.")
    @GetMapping("/{factoryId}/material/order")
    public ResponseEntity<ApiResponse<PageResponseDto<MaterialOrderResponseDto>>> getMaterialOrders(
            @PathVariable Long factoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK,
                materialOrderService.getMaterialOrdersByFactory(factoryId, page, size));
    }

    @Operation(summary = "자재 주문 입고 처리", description = "자재 주문을 입고 처리합니다.")
    @PutMapping("/{factoryId}/material/order/{orderId}/receive")
    public ResponseEntity<ApiResponse<MaterialOrderResponseDto>> receiveMaterialOrder(
            @PathVariable Long factoryId,
            @PathVariable Long orderId) {
        return ApiResponse.success(SuccessStatus.OK,
                materialOrderService.receiveMaterialOrder(factoryId, orderId));
    }
}