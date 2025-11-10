package com.sampoom.factory.api.mps.controller;

import com.sampoom.factory.api.mps.dto.MpsPlanResponse;
import com.sampoom.factory.api.mps.entity.MpsPlan;
import com.sampoom.factory.api.mps.service.MpsPlanService;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MPS Plan", description = "MPS 상세 생산 계획 관리 API")
@RestController
@RequestMapping("/{factoryId}/mps-plans")
@RequiredArgsConstructor
@Slf4j
public class MpsPlanController {

    private final MpsPlanService mpsPlanService;

    @Operation(summary = "MpsPlan 상세 조회", description = "특정 MpsPlan의 상세 정보를 조회합니다.")
    @GetMapping("/{mpsPlanId}")
    public ResponseEntity<ApiResponse<MpsPlanResponse>> getMpsPlan(
            @Parameter(description = "MPS Plan ID") @PathVariable Long mpsPlanId) {

        MpsPlan mpsPlan = mpsPlanService.getMpsPlanById(mpsPlanId);
        return ApiResponse.success(SuccessStatus.OK, MpsPlanResponse.from(mpsPlan));
    }
}
