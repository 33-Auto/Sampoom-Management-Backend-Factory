package com.sampoom.factory.api.part.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.part.dto.PartOrderRequestDto;
import com.sampoom.factory.api.part.dto.PartOrderResponseDto;
import com.sampoom.factory.api.part.entity.PartOrderStatus;
import com.sampoom.factory.api.part.service.PartOrderService;
import com.sampoom.factory.common.response.SuccessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartOrderApiController.class)
class PartOrderApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PartOrderService partOrderService;

    @Test
    @DisplayName("부품 주문 자동 생성 API 테스트")
    void createPartOrderTest() throws Exception {
        // Given
        PartOrderRequestDto requestDto = createPartOrderRequestDto();
        PartOrderResponseDto responseDto = createPartOrderResponseDto();

        given(partOrderService.createPartOrder(any(PartOrderRequestDto.class)))
                .willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/part/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(SuccessStatus.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(responseDto.getOrderId()))
                .andExpect(jsonPath("$.data.factoryId").value(responseDto.getFactoryId()))
                .andExpect(jsonPath("$.data.factoryName").value(responseDto.getFactoryName()))
                .andExpect(jsonPath("$.data.status").value(responseDto.getStatus()));
    }

    private PartOrderRequestDto createPartOrderRequestDto() {
        PartOrderRequestDto.PartOrderItemRequestDto item = PartOrderRequestDto.PartOrderItemRequestDto.builder()
                .partId(1L)
                .quantity(10L)
                .build();

        return PartOrderRequestDto.builder()
                .warehouseName("테스트 창고")
                .items(List.of(item))
                .build();
    }

    private PartOrderResponseDto createPartOrderResponseDto() {
        PartOrderResponseDto.PartOrderItemDto itemDto = PartOrderResponseDto.PartOrderItemDto.builder()
                .partId(1L)
                .partName("테스트 부품")
                .partCode("P001")
                .partGroup("테스트 그룹")
                .partCategory("테스트 카테고리")
                .quantity(10L)
                .build();

        return PartOrderResponseDto.builder()
                .orderId(1L)
                .warehouseName("테스트 창고")
                .orderDate(LocalDateTime.now())
                .status(PartOrderStatus.UNDER_REVIEW.name())
                .factoryId(1L)
                .factoryName("테스트 공장")
                .items(List.of(itemDto))
                .build();
    }
}