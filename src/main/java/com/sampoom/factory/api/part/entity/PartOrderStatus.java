package com.sampoom.factory.api.part.entity;

public enum PartOrderStatus {
    UNDER_REVIEW,       // 검토중
    PURCHASE_REQUEST,   // 구매요청
    PLAN_CONFIRMED,     // 계획확정
    DELAYED,            // 지연
    REJECTED,           // 반려
    IN_PROGRESS,        // 진행중
    COMPLETED           // 완료
}
