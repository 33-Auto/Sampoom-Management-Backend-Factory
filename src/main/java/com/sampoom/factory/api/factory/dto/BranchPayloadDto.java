package com.sampoom.factory.api.factory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchPayloadDto {
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String status;
    private Boolean deleted;
}
