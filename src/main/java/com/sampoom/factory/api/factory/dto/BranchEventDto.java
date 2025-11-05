package com.sampoom.factory.api.factory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchEventDto {
    private String eventId;
    private String eventType;
    private Integer version;
    private LocalDateTime occurredAt;
    private BranchPayloadDto payload;
}
