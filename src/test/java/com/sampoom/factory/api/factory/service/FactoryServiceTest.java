package com.sampoom.factory.api.factory.service;

import com.sampoom.factory.api.factory.dto.FactoryCreateRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryRequestDto;
import com.sampoom.factory.api.factory.dto.FactoryResponseDto;
import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.api.factory.repository.FactoryRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.material.repository.MaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactoryServiceTest {

    @Mock private FactoryRepository factoryRepository;
    @Mock private MaterialRepository materialRepository; // read-only면 MaterialReadRepository로 바꿔도 됨
    @Mock private FactoryMaterialRepository factoryMaterialRepository;

    @InjectMocks private FactoryService factoryService;

    @Test
    @DisplayName("공장 생성 시 모든 자재가 수량 0으로 연결된다")
    void createFactory_ShouldCreateFactoryWithAllMaterialsQuantityZero() {
        // Given
        var requestDto = new FactoryRequestDto("테스트 공장", "테스트 위치");
        var factory = Factory.builder().id(1L).name("테스트 공장").address("테스트 위치").build();

        // Material은 읽기 전용 → mock으로 id만 스텁
        Material material1 = mock(Material.class);
        Material material2 = mock(Material.class);
        when(material1.getId()).thenReturn(1L);
        when(material2.getId()).thenReturn(2L);

        when(factoryRepository.save(any(Factory.class))).thenReturn(factory);
        when(materialRepository.findAll()).thenReturn(List.of(material1, material2));

        // When
        FactoryResponseDto responseDto = factoryService.createFactory(requestDto);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(1L);

        // 저장된 FactoryMaterial들의 실제 값 검증
        var captor = ArgumentCaptor.forClass(FactoryMaterial.class);
        verify(factoryMaterialRepository, times(2)).save(captor.capture());
        var saved = captor.getAllValues();

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(fm -> {
            assertThat(fm.getFactory().getId()).isEqualTo(1L);
            assertThat(fm.getQuantity()).isZero();
        });
        assertThat(saved.stream().map(FactoryMaterial::getMaterial).map(Material::getId).toList())
                .containsExactlyInAnyOrder(1L, 2L);
    }
}