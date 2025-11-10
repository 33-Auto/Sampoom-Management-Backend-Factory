package com.sampoom.factory.api.part.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartOrderType {
    GENERAL("일반주문"),
    MPS("MPS주문");

    private final String description;
}
