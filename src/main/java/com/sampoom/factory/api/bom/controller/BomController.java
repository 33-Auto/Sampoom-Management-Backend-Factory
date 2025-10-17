package com.sampoom.factory.api.bom.controller;

import com.sampoom.factory.api.bom.dto.BomDetailResponseDto;
import com.sampoom.factory.api.bom.dto.BomRequestDto;
import com.sampoom.factory.api.bom.dto.BomResponseDto;
import com.sampoom.factory.api.bom.service.BomService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.PageResponseDto;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BOM", description = "BOM(Bill of Materials) 관련 API 입니다.")
@RestController
@RequestMapping("/bom")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @Operation(summary = "BOM 추가", description = "새로운 BOM을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BomResponseDto>> createBom(@RequestBody BomRequestDto requestDto) {
        return ApiResponse.success(SuccessStatus.CREATED, bomService.createBom(requestDto));
    }

    @Operation(summary = "BOM 목록 조회", description = "페이징 처리된 BOM 목록을 조회하고, 카테고리나 그룹으로 필터링합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<BomResponseDto>>> getBoms(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK, bomService.searchBoms(null, categoryId, groupId, page, size));
    }

    @Operation(summary = "BOM 상세 조회", description = "특정 BOM의 상세 정보를 조회합니다.")
    @GetMapping("/{bomId}")
    public ResponseEntity<ApiResponse<BomDetailResponseDto>> getBomDetail(@PathVariable Long bomId) {
        return ApiResponse.success(SuccessStatus.OK, bomService.getBomDetail(bomId));
    }

    @Operation(summary = "BOM 수정", description = "특정 BOM 정보를 수정합니다.")
    @PutMapping("/{bomId}")
    public ResponseEntity<ApiResponse<BomResponseDto>> updateBom(
            @PathVariable Long bomId,
            @RequestBody BomRequestDto requestDto) {
        return ApiResponse.success(SuccessStatus.OK, bomService.updateBom(bomId, requestDto));
    }

    @Operation(summary = "BOM 삭제", description = "특정 BOM을 삭제합니다.")
    @DeleteMapping("/{bomId}")
    public ResponseEntity<ApiResponse<Void>> deleteBom(@PathVariable Long bomId) {
        bomService.deleteBom(bomId);
        return ApiResponse.success_only(SuccessStatus.OK);
    }

    @Operation(summary = "BOM 검색", description = "부품 이름 또는 부품 코드로 BOM을 검색하고, 카테고리나 그룹으로 필터링합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDto<BomResponseDto>>> searchBoms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK,
                bomService.searchBoms(keyword, categoryId, groupId, page, size));
    }
}