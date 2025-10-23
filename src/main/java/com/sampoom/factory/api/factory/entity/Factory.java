package com.sampoom.factory.api.factory.entity;


import com.sampoom.factory.api.factory.dto.FactoryRequestDto;
import com.sampoom.factory.common.entitiy.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "factory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE factory " +
        "SET deleted = true, deleted_at = now() " +
        "WHERE factory_id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Factory extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_id")
    private Long id;

    @Column(name = "factory_name",nullable = false)
    private String name;  // 지점명

    @Column(nullable = true)
    private String address;  // 주소

    @Enumerated(EnumType.STRING)
    private FactoryStatus  status;

    @Version
    private Long version; // JPA가 자동 관리 (낙관적 락 + 자동 증가)

    @PrePersist
    void prePersist() {
        if (status == null) status = FactoryStatus.ACTIVE; // 기본값
    }

    public void update(FactoryRequestDto dto) {
        this.name = dto.getName();
        this.address = dto.getAddress();
    }


    public void inactivate() {
        this.status = FactoryStatus.INACTIVE;
    }



}
