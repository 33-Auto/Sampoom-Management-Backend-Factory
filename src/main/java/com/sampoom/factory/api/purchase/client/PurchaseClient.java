package com.sampoom.factory.api.purchase.client;

import com.sampoom.factory.api.purchase.dto.PurchaseRequestDto;
import com.sampoom.factory.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "purchase-service",
        url = "${external.purchase.api.url}",
        fallback = PurchaseClientFallback.class
)
public interface PurchaseClient {

    @PostMapping
    ApiResponse<Void> createPurchaseRequest(@RequestBody PurchaseRequestDto request);
}
