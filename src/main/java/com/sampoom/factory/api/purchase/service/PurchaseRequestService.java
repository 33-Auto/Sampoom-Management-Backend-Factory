package com.sampoom.factory.api.purchase.service;

import com.sampoom.factory.api.purchase.client.PurchaseClient;
import com.sampoom.factory.api.purchase.dto.PurchaseRequestDto;
import com.sampoom.factory.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseClient purchaseClient;

    public void sendPurchaseRequest(PurchaseRequestDto purchaseRequest) {
        try {
            log.info("구매요청 API 호출 시작 - 공장ID: {}, 아이템 수: {}",
                purchaseRequest.getFactoryId(), purchaseRequest.getItems().size());

            // Feign Client를 통한 API 호출
            ApiResponse<Void> response = purchaseClient.createPurchaseRequest(purchaseRequest);

            if (response.getSuccess()) {
                log.info("구매요청 API 호출 성공 - 공장ID: {}", purchaseRequest.getFactoryId());
            } else {
                log.warn("구매요청 API 호출 실패 - 응답: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("구매요청 API 호출 중 예외 발생: {}", e.getMessage(), e);
            // 예외가 발생해도 전체 주문 프로세스는 계속 진행
        }
    }
}
