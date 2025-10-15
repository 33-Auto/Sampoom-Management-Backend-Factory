package com.sampoom.factory.api.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.api.bom.dto.BomDetailResponseDto;
import com.sampoom.factory.api.bom.dto.BomRequestDto;
import com.sampoom.factory.api.bom.dto.BomResponseDto;
import com.sampoom.factory.api.bom.service.BomService;
import com.sampoom.factory.common.response.PageResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BomController.class)
class BomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BomService bomService;

    @Test
    @DisplayName("BOM 추가 테스트")
    void createBom() throws Exception {
        BomRequestDto requestDto = BomRequestDto.builder()
                .partId(1L)
                .materials(Collections.emptyList())
                .build();

        BomResponseDto responseDto = BomResponseDto.builder()
                .id(1L)
                .partId(1L)
                .partName("Part A")

                .build();

        Mockito.when(bomService.createBom(any(BomRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/bom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.partId").value(1L))
                .andExpect(jsonPath("$.data.partName").value("Part A"));
    }

    @Test
    @DisplayName("BOM 목록 조회 테스트")
    void getBoms() throws Exception {
        PageResponseDto<BomResponseDto> pageResponse = PageResponseDto.<BomResponseDto>builder()
                .content(Collections.emptyList())
                .totalPages(1)
                .totalElements(0)
                .build();

        Mockito.when(bomService.getBoms(anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/bom")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

    }


    @Test
    @DisplayName("BOM 상세 조회 테스트")
    void getBomDetail() throws Exception {
        BomDetailResponseDto responseDto = BomDetailResponseDto.builder()
                .id(1L)
                .partId(1L)
                .partName("Part A")
                .materials(Collections.emptyList())
                .build();

        Mockito.when(bomService.getBomDetail(anyLong())).thenReturn(responseDto);

        mockMvc.perform(get("/bom/{bomId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.partName").value("Part A"));
    }
    @Test
    @DisplayName("BOM 수정 테스트")
    void updateBom() throws Exception {
        BomRequestDto requestDto = BomRequestDto.builder()
                .partId(1L)
                .materials(Collections.emptyList())
                .build();

        BomResponseDto responseDto = BomResponseDto.builder()
                .id(1L)
                .partId(1L)
                .partName("Updated Part")

                .build();

        Mockito.when(bomService.updateBom(anyLong(), any(BomRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/bom/{bomId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partName").value("Updated Part"));
    }

    @Test
    @DisplayName("BOM 삭제 테스트")
    void deleteBom() throws Exception {
        mockMvc.perform(delete("/bom/{bomId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("BOM 검색 테스트")
    void searchBoms() throws Exception {
        PageResponseDto<BomResponseDto> pageResponse = PageResponseDto.<BomResponseDto>builder()
                .content(Collections.emptyList())
                .totalPages(1)
                .totalElements(0)
                .build();

        Mockito.when(bomService.searchBoms(any(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/bom/search")
                        .param("keyword", "Part")
                        .param("partId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

    }
}