package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "factory_material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FactoryMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

    @Column(name = "material_id")
    private Long materialId;

    private Long quantity;

    public void increaseQuantity(Long amount) {
        this.quantity += amount;
    }

    public void decreaseQuantity(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException(ErrorStatus.INVALID_QUANTITY);
        }
        if (this.quantity == null) {
            this.quantity = 0L;
        }
        long newQty = this.quantity - amount;
        if (newQty < 0) {
            throw new BadRequestException(ErrorStatus.INSUFFICIENT_MATERIAL_QUANTITY);
        }
        this.quantity = newQty;
    }
}
