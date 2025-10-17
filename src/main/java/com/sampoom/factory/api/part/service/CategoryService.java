package com.sampoom.factory.api.part.service;

import com.sampoom.factory.api.part.dto.CategoryResponseDto;
import com.sampoom.factory.api.part.dto.PartGroupResponseDto;
import com.sampoom.factory.api.part.entity.Category;
import com.sampoom.factory.api.part.repository.CategoryRepository;
import com.sampoom.factory.api.part.repository.PartGroupRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.ErrorStatus;
import com.sampoom.factory.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PartGroupRepository partGroupRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartGroupResponseDto> getGroupsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CATEGORY_NOT_FOUND));

        return partGroupRepository.findByCategory(category).stream()
                .map(PartGroupResponseDto::from)
                .collect(Collectors.toList());
    }
}