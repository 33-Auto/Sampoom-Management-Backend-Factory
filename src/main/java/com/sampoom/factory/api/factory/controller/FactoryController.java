package com.sampoom.factory.api.factory.controller;

import com.sampoom.factory.api.factory.dto.FactoryCreateRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryResponseDto;
import com.sampoom.factory.api.factory.service.FactoryService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Factory", description = "Factory 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @Operation(summary = "공장 생성", description = "공장을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FactoryResponseDto>> createFactory(@RequestBody FactoryCreateRequestDto requestDto) {
        FactoryResponseDto responseDto = factoryService.createFactory(requestDto);
        return ApiResponse.success(SuccessStatus.CREATED,responseDto);
    }
}
