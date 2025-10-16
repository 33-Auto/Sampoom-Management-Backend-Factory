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
@RequestMapping()
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


    @Operation(
            summary = "공장별 자재 검색/목록 조회",
            description = "특정 공장의 자재를 페이징 조회합니다. 카테고리(categoryId)로 필터링하고, keyword(자재명/자재코드)로 검색합니다."
    )
    @GetMapping("/{factoryId}/material")
    public ResponseEntity<ApiResponse<PageResponseDto<MaterialResponseDto>>> getMaterials(
            @PathVariable Long factoryId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponse.success(
                SuccessStatus.OK,
                factoryMaterialService.searchMaterials(factoryId, categoryId, keyword, page, size)
        );
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

    @Operation(
            summary = "자재 주문 취소",
            description = "특정 공장의 자재 주문을 취소합니다. (받은(입고) 주문은 취소 불가)"
    )
    @PutMapping("/{factoryId}/material/order/{orderId}/cancel")
    public ResponseEntity<ApiResponse<MaterialOrderResponseDto>> cancelMaterialOrder(
            @PathVariable Long factoryId,
            @PathVariable Long orderId) {

        return ApiResponse.success(
                SuccessStatus.OK,
                materialOrderService.cancelMaterialOrder(factoryId, orderId)
        );
    }

    @Operation(summary = "자재 주문 삭제(소프트)", description = "주문 레코드를 실제로는 삭제하지 않고 숨깁니다.")
    @DeleteMapping("/{factoryId}/material/order/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteMaterialOrder(
            @PathVariable Long factoryId, @PathVariable Long orderId) {
        materialOrderService.softDeleteMaterialOrder(factoryId, orderId);
        return ApiResponse.success_only(SuccessStatus.OK);
    }
}