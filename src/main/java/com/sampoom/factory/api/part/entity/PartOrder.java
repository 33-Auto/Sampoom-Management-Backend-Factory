package com.sampoom.factory.api.part.entity;

import com.sampoom.factory.api.factory.entity.Factory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "part_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;

    @Enumerated(EnumType.STRING)
    private PartOrderStatus status;

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "partOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PartOrderItem> items = new ArrayList<>();

    public void updateStatus(PartOrderStatus status) {
        this.status = status;
    }
}