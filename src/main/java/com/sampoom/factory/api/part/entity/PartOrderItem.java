package com.sampoom.factory.api.part.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "part_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PartOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_order_id", nullable = false)
    private PartOrder partOrder;

    @Column(name = "part_id", nullable = false)
    private Long partId;

    @Column(nullable = false)
    private Long quantity;
}