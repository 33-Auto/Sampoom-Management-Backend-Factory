package com.sampoom.factory.common.config;

import com.opencsv.CSVReader;
import com.sampoom.factory.api.material.entity.Material;
import com.sampoom.factory.api.material.entity.MaterialCategory;
import com.sampoom.factory.api.material.repository.MaterialCategoryRepository;
import com.sampoom.factory.api.material.repository.MaterialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final MaterialRepository materialRepository;
    private final MaterialCategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (materialRepository.count() > 0) {
            log.info("Material data already exists, skipping import.");
            return;
        }

        log.info("Importing CSV data into PostgreSQL...");

        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/data/materials_master_cleaned.csv")),
                "UTF-8");
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();
            // 첫 줄 헤더 제거
            rows.remove(0);

            Map<Long, MaterialCategory> categoryCache = new HashMap<>();

            for (String[] row : rows) {
                Long id = Long.parseLong(row[0]);
                Long categoryId = Long.parseLong(row[1]);
                String categoryName = row[2];
                String code = row[3];
                String name = row[4];

                // 카테고리 처리 로직 수정
                MaterialCategory category = categoryCache.get(categoryId);
                if (category == null) {
                    // DB에서 먼저 찾고, 없으면 새로 생성
                    category = categoryRepository.findById(categoryId).orElse(null);
                    if (category == null) {
                        // 카테고리 ID에 따라 적절한 접두사 선택
                        String prefix;
                        switch(categoryId.intValue()) {
                            case 1:
                                prefix = "MTL";
                                break;
                            case 2:
                                prefix = "PLS";
                                break;
                            case 3:
                                prefix = "ELC";
                                break;
                            case 4:
                                prefix = "CHM";
                                break;
                            default:
                                prefix = "CAT";
                                break;
                        }

                        category = MaterialCategory.builder()
                                .name(categoryName)
                                .code(prefix)
                                .build();
                        // ID는 명시적으로 설정하지 않음 (자동 생성)
                        category = categoryRepository.save(category);
                    }
                    categoryCache.put(categoryId, category);
                }

                // 자재 저장 (기존과 동일)
                Material material = Material.builder()
                        .name(name)
                        .code(code)
                        .materialCategory(category)
                        .build();

                materialRepository.save(material);
            }

            log.info("CSV import completed. Inserted materials: " + materialRepository.count());


        }
    }
}