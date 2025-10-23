package com.sampoom.factory.api.factory.dto;

import com.sampoom.factory.api.factory.entity.Factory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FactoryResponseDto {
    private Long id;
    private String name;
    private String address;

    public static FactoryResponseDto from(Factory factory) {
        return FactoryResponseDto.builder()
                .id(factory.getId())
                .name(factory.getName())
                .address(factory.getAddress())
                .build();
    }
}