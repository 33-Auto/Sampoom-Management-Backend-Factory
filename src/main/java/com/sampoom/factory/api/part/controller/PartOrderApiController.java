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

import java.util.List;

@RestController
@RequiredArgsConstructor

@Tag(name = "PartOrder", description = "PartOrder 관련 API 입니다.")
public class PartOrderApiController {

    private final PartOrderService partOrderService;

    @Operation(summary = "부품 주문 생성", description = "적절한 공장을 자동으로 선택하여 부품 주문을 생성합니다. 여러 아이템이 있는 경우 각각 단건으로 나누어 개별 주문을 생성합니다.")
    @PostMapping("/part/order")
    public ResponseEntity<ApiResponse<List<PartOrderResponseDto>>> createPartOrder(
            @RequestBody PartOrderRequestDto request
    ) {
        List<PartOrderResponseDto> response = partOrderService.createPartOrdersSeparately(request);
        return ApiResponse.success(SuccessStatus.CREATED, response);
    }


}
