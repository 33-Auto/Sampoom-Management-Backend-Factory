package com.sampoom.factory.api.health;

import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.exception.ForbiddenException;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.exception.UnauthorizedException;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.ErrorStatus;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HealthCheck", description = "HealthCheck 관련 API 입니다.")
@RestController
@RequestMapping()
public class HealthCheckController {


    @GetMapping("/health")
    /* Swagger 작성예시     */
    @Operation(
            summary = "HealthCheck API",
            description = "서버 상태 체크 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서버 상태 OK"),
    })

    // - 데이터 없이 응답 코드 + 메시지만 반환할 경우 사용
    // - ApiResponse.success_only(성공 상태 enum)
    public ResponseEntity<ApiResponse<Void>> healthCheck() {
        return ApiResponse.success_only(SuccessStatus.OK);
    }

    // - 응답 코드 + 메시지 + 실제 데이터 모두 반환할 경우 사용
    // - ApiResponse.success(성공 상태 enum, 데이터)
    @GetMapping("/health-data")
    public ResponseEntity<ApiResponse<String>> healthCheckData() {
        return ApiResponse.success(SuccessStatus.OK, "DATA");
    }


    /**
     * @param fail
     * @return  예외 테스트: 파라미터가 true 이면 BadRequestException; 정상일 경우 응답 메시지
     */
    @GetMapping("/health-error")
    public ResponseEntity<ApiResponse<Void>> healthCheckData(@RequestParam(required = false) Boolean fail) {
        if (Boolean.TRUE.equals(fail)) {
            throw new BadRequestException(ErrorStatus.BAD_REQUEST.getMessage());
        }

        return ApiResponse.success_only(SuccessStatus.OK);
    }

    /**
     * 예외 처리 예시
     */
    @GetMapping("/health-notfound")
    public ResponseEntity<ApiResponse<Void>> notFoundTest() {
        throw new NotFoundException(ErrorStatus.NOT_FOUND.getMessage());
    }

    @GetMapping("/health-unauthorized")
    public ResponseEntity<ApiResponse<Void>> unauthorizedTest() {
        throw new UnauthorizedException(ErrorStatus.UNAUTHORIZED.getMessage());
    }

    @GetMapping("/health-forbidden")
    public ResponseEntity<ApiResponse<Void>> forbiddenTest() {
        throw new ForbiddenException(ErrorStatus.FORBIDDEN.getMessage());
    }


}
