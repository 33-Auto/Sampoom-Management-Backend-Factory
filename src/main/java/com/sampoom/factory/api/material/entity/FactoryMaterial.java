package com.sampoom.factory.api.material.entity;

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

    @Column(name = "factory_id")
    private Long factoryId;

    @Column(name = "material_id")
    private Long materialId;

    private Double quantity;  // Long에서 Double로 변경

    public void increaseQuantity(Double amount) {  // Long에서 Double로 변경
        if (this.quantity == null) {
            this.quantity = 0.0;
        }
        this.quantity += amount;
    }

    public void decreaseQuantity(Double amount) {  // Long에서 Double로 변경
        if (amount == null || amount <= 0) {
            throw new BadRequestException(ErrorStatus.INVALID_QUANTITY);
        }
        if (this.quantity == null) {
            this.quantity = 0.0;
        }
        double newQty = this.quantity - amount;
        if (newQty < 0) {
            throw new BadRequestException(ErrorStatus.INSUFFICIENT_MATERIAL_QUANTITY);
        }
        this.quantity = newQty;
    }
}
