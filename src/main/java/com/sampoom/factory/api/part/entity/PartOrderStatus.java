package com.sampoom.factory.api.part.entity;

public enum PartOrderStatus {
    REQUESTED,      // 주문 접수
    IN_PRODUCTION,  // 생산중
    COMPLETED,      // 생산완료
    DELIVERING,     // 배달중
    DELIVERED,      // 배달완료
    LACK_OF_MATERIAL, // 자재 부족
    CANCELED        // 주문 취소

}
