package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.entity.PartOrder;
import com.sampoom.factory.api.part.repository.PartOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartOrderCodeGenerator {

    private final PartOrderRepository partOrderRepository;

    /**
     * WO-2025-001 형태의 주문 코드를 생성합니다.
     * WO: Work Order의 약자
     * 2025: 현재 년도
     * 001: 해당 년도의 순차 번호 (3자리)
     */
    public String generateOrderCode() {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));

        // 해당 년도의 마지막 주문 코드 조회
        String lastOrderCode = partOrderRepository.findTopByOrderCodeStartingWithOrderByOrderCodeDesc("WO-" + year + "-")
                .map(PartOrder::getOrderCode)
                .orElse(null);

        int nextSequence = 1;
        if (lastOrderCode != null && !lastOrderCode.isEmpty()) {
            // WO-2025-001에서 마지막 3자리 숫자 추출
            String[] parts = lastOrderCode.split("-");
            if (parts.length == 3) {
                try {
                    int lastSequence = Integer.parseInt(parts[2]);
                    nextSequence = lastSequence + 1;
                } catch (NumberFormatException e) {
                    log.warn("주문 코드 파싱 실패: {}", lastOrderCode);
                }
            }
        }

        // WO-2025-001 형태로 생성
        String orderCode = String.format("WO-%s-%03d", year, nextSequence);

        log.info("주문 코드 생성: {}", orderCode);
        return orderCode;
    }
}
