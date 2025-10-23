package com.sampoom.factory.api.factory.dto;

import com.sampoom.factory.api.factory.entity.Factory;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FactoryCreateRequestDto {

    @NotBlank(message = "공장 이름은 필수입니다")
    private String name;

    @NotBlank(message = "공장 위치는 필수입니다")
    private String location;

    public Factory toEntity() {
        return Factory.builder()
                .name(this.name)
                .address(this.location)
                .build();
    }
}