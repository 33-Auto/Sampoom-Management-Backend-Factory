package com.sampoom.factory.common.response;

import lombok.*;

import java.util.List;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    private List<T> content;         // 실제 데이터
    private long totalElements;      // 총 요소 수
    private int totalPages;          // 총 페이지 수
}