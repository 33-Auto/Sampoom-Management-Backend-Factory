package com.sampoom.factory.api.mps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpsPartInfoDto {
    private Long partId;
    private String partCode;
    private String partName;
}
