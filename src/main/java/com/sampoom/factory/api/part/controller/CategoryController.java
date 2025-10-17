package com.sampoom.factory.api.part.controller;

import com.sampoom.factory.api.part.dto.CategoryResponseDto;
import com.sampoom.factory.api.part.dto.PartGroupResponseDto;
import com.sampoom.factory.api.part.service.CategoryService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category", description = "카테고리 관련 API 입니다.")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategories() {
        return ApiResponse.success(SuccessStatus.OK, categoryService.getAllCategories());
    }

    @Operation(summary = "카테고리별 그룹 조회", description = "특정 카테고리에 속한 그룹 목록을 조회합니다.")
    @GetMapping("/{categoryId}/groups")
    public ResponseEntity<ApiResponse<List<PartGroupResponseDto>>> getGroupsByCategory(
            @PathVariable Long categoryId) {
        return ApiResponse.success(SuccessStatus.OK, categoryService.getGroupsByCategory(categoryId));
    }
}