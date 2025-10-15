package com.sampoom.factory.api.part.entity;

import com.sampoom.factory.common.entitiy.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "part_group")
@Getter
@NoArgsConstructor
public class PartGroup extends BaseTimeEntity {

    @Id
    private Long id;

    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}