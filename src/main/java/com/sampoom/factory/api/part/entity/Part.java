package com.sampoom.factory.api.part.entity;

import com.sampoom.factory.common.entitiy.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "part")
@Getter
@NoArgsConstructor
@Immutable
public class Part extends BaseTimeEntity{

    @Id
    private Long id;

    private String code;
    private String name;
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private PartGroup group;
}