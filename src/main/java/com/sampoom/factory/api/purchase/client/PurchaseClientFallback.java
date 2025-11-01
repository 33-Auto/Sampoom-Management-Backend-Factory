package com.sampoom.factory.api.purchase.client;

import com.sampoom.factory.api.purchase.dto.PurchaseRequestDto;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PurchaseClientFallback implements PurchaseClient {

    @Override
    public ApiResponse<Void> createPurchaseRequest(PurchaseRequestDto request) {
        log.error("구매요청 API 호출 실패 - Fallback 실행. 공장ID: {}, 아이템 수: {}",
                request.getFactoryId(), request.getItems().size());

        // Fallback 시에도 주문 프로세스는 계속 진행되도록 처리
        // 실제로는 별도의 재시도 큐에 넣거나, 관리자 알림을 보낼 수 있음

        return ApiResponse.fail_only(ErrorStatus.EXTERNAL_API_ERROR);
    }
}
