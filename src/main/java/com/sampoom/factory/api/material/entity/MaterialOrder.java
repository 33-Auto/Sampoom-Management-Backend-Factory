package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "material_order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_order_id")
    private Long id;

    private String code;
    private LocalDateTime orderAt;
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

    public MaterialOrder receive() {
        if (this.status != OrderStatus.ORDERED) {
            throw new BadRequestException(ErrorStatus.ORDER_ALREADY_PROCESSED);
        }
        this.status = OrderStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
        return this;
    }
}
