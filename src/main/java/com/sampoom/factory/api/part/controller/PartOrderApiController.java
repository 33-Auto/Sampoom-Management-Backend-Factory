package com.sampoom.factory.api.part.controller;


import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.service.PartOrderService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor

@Tag(name = "PartOrder", description = "PartOrder 관련 API 입니다.")
public class PartOrderApiController {

    private final PartOrderService partOrderService;

    @Operation(summary = "부품 주문 생성", description = "적절한 공장을 자동으로 선택하여 부품 주문을 생성합니다.")
    @PostMapping("/part/order")
    public ResponseEntity<ApiResponse<PartOrderResponseDto>> createPartOrder(
            @RequestBody PartOrderRequestDto request
    ) {
        PartOrderResponseDto response = partOrderService.createPartOrder(request);
        return ApiResponse.success(SuccessStatus.CREATED, response);
    }


}
