package com.sampoom.factory.api.part.controller;

import com.sampoom.factory.api.part.dto.CategoryResponseDto;
import com.sampoom.factory.api.part.dto.PartGroupResponseDto;
import com.sampoom.factory.api.part.service.CategoryService;
import com.sampoom.factory.common.response.SuccessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("모든 카테고리 조회 테스트")
    void getAllCategoriesTest() throws Exception {
        // given
        List<CategoryResponseDto> categories = Arrays.asList(
                CategoryResponseDto.builder().id(1L).code("CAT001").name("전자부품").build(),
                CategoryResponseDto.builder().id(2L).code("CAT002").name("기계부품").build()
        );

        when(categoryService.getAllCategories()).thenReturn(categories);

        // when & then
        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SuccessStatus.OK.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("CAT001"))
                .andExpect(jsonPath("$.data[0].name").value("전자부품"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].code").value("CAT002"))
                .andExpect(jsonPath("$.data[1].name").value("기계부품"));
    }

    @Test
    @DisplayName("카테고리별 그룹 조회 테스트")
    void getGroupsByCategoryTest() throws Exception {
        // given
        Long categoryId = 1L;
        List<PartGroupResponseDto> groups = Arrays.asList(
                PartGroupResponseDto.builder()
                        .id(1L)
                        .code("GRP001")
                        .name("반도체")
                        .categoryId(categoryId)
                        .categoryName("전자부품")
                        .build(),
                PartGroupResponseDto.builder()
                        .id(2L)
                        .code("GRP002")
                        .name("저항")
                        .categoryId(categoryId)
                        .categoryName("전자부품")
                        .build()
        );

        when(categoryService.getGroupsByCategory(categoryId)).thenReturn(groups);

        // when & then
        mockMvc.perform(get("/categories/{categoryId}/groups", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SuccessStatus.OK.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("GRP001"))
                .andExpect(jsonPath("$.data[0].name").value("반도체"))
                .andExpect(jsonPath("$.data[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.data[0].categoryName").value("전자부품"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].code").value("GRP002"))
                .andExpect(jsonPath("$.data[1].name").value("저항"));
    }
}