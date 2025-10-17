package com.sampoom.factory.api.material.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.material.dto.MaterialOrderResponseDto;
import com.sampoom.factory.api.material.dto.MaterialResponseDto;
import com.sampoom.factory.api.material.service.FactoryMaterialService;
import com.sampoom.factory.api.material.service.MaterialOrderService;
import com.sampoom.factory.common.response.PageResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FactoryMaterialController.class)
class FactoryMaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FactoryMaterialService factoryMaterialService;

    @MockitoBean
    private MaterialOrderService materialOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("자재 검색/목록 조회")
    void getMaterials() throws Exception {
        PageResponseDto<MaterialResponseDto> page = PageResponseDto.<MaterialResponseDto>builder()
                .content(Collections.emptyList())
                .totalElements(0)
                .totalPages(0)
                .build();

        Mockito.when(factoryMaterialService.searchMaterials(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/1/material")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("자재 주문 삭제")
    void deleteMaterialOrder() throws Exception {
        Mockito.doNothing().when(materialOrderService).softDeleteMaterialOrder(Mockito.anyLong(), Mockito.anyLong());

        mockMvc.perform(delete("/1/material/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("자재 주문 취소")
    void cancelMaterialOrder() throws Exception {
        MaterialOrderResponseDto responseDto = Mockito.mock(MaterialOrderResponseDto.class);
        Mockito.when(materialOrderService.cancelMaterialOrder(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(responseDto);

        mockMvc.perform(put("/1/material/order/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("자재 주문 상세조회")
    void getMaterialOrderDetail() throws Exception {
        MaterialOrderResponseDto responseDto = Mockito.mock(MaterialOrderResponseDto.class);
        Mockito.when(materialOrderService.getMaterialOrderDetail(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(responseDto);

        mockMvc.perform(get("/1/material/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}