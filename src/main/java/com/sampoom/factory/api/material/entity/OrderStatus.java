package com.sampoom.factory.api.material.entity;

import lombok.Getter;


public enum OrderStatus {
    ORDERED,    // 주문됨
    RECEIVED,    // 입고됨
    CANCELED   // 발주 취소됨
}
