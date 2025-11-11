package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.common.entity.SoftDeleteEntity;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "material_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE material_order SET deleted = true, deleted_at = now() WHERE material_order_id = ?")
@SQLRestriction("deleted = false")
public class MaterialOrder extends SoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_order_id")
    private Long id;

    private String code;
    private LocalDateTime orderAt;
    private LocalDateTime receivedAt;
    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "factory_id")
    private Long factoryId;

    public void receive() {
        if (this.status != OrderStatus.ORDERED) {
            throw new BadRequestException(ErrorStatus.ORDER_ALREADY_PROCESSED);
        }
        this.status = OrderStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public void cancel() {

        if (this.status != OrderStatus.ORDERED) {
            throw new BadRequestException(ErrorStatus.ORDER_ALREADY_PROCESSED);
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

}
