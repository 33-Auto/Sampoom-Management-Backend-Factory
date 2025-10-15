package com.sampoom.factory.api.factory.dto;

import com.sampoom.factory.api.factory.entity.Factory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FactoryCreateRequestDto {
    private String name;
    private String location;

    public Factory toEntity() {
        return Factory.builder()
                .name(this.name)
                .location(this.location)
                .build();
    }
}