package com.sampoom.factory.api.part.entity;

import com.sampoom.factory.common.entitiy.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor
public class Category extends BaseTimeEntity{

    @Id
    private Long id;

    private String code;
    private String name;
}