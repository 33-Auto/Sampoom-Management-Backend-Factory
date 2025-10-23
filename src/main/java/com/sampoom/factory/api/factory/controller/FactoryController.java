package com.sampoom.factory.api.factory.controller;


import com.sampoom.factory.api.factory.dto.FactoryRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryResponseDto;
import com.sampoom.factory.api.factory.service.FactoryService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Factory", description = "Factory 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @Operation(summary = "공장 생성", description = "공장을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FactoryResponseDto>> createFactory(@Valid @RequestBody FactoryRequestDto requestDto) {
        FactoryResponseDto responseDto = factoryService.createFactory(requestDto);
        return ApiResponse.success(SuccessStatus.CREATED,responseDto);
    }

    @Operation(summary = "공장 수정", description = "공장 정보를 수정합니다.")
    @PutMapping("/{factoryId}")
    public ResponseEntity<ApiResponse<FactoryResponseDto>> updateFactory(
            @PathVariable Long factoryId,
            @Valid @RequestBody FactoryRequestDto requestDto) {
        FactoryResponseDto responseDto = factoryService.updateFactory(factoryId, requestDto);
        return ApiResponse.success(SuccessStatus.OK, responseDto);
    }

    @Operation(summary = "공장 삭제", description = "공장을 삭제합니다.")
    @DeleteMapping("/{factoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteFactory(@PathVariable Long factoryId) {
        factoryService.deleteFactory(factoryId);
        return ApiResponse.success_only(SuccessStatus.OK);
    }

    @Operation(summary = "공장 조회", description = "공장 정보를 조회합니다.")
    @GetMapping("/{factoryId}")
    public ResponseEntity<ApiResponse<FactoryResponseDto>> getFactory(@PathVariable Long factoryId) {
        FactoryResponseDto responseDto = factoryService.getFactory(factoryId);
        return ApiResponse.success(SuccessStatus.OK, responseDto);
    }

    @Operation(summary = "공장 목록 조회", description = "모든 공장 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FactoryResponseDto>>> getAllFactories() {
        List<FactoryResponseDto> responseDtos = factoryService.getAllFactories();
        return ApiResponse.success(SuccessStatus.OK, responseDtos);
    }
}
