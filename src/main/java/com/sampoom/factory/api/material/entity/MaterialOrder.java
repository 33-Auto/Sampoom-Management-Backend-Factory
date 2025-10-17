package com.sampoom.factory.api.material.entity;

import com.sampoom.factory.api.factory.entity.Factory;
import com.sampoom.factory.common.entitiy.SoftDeleteEntity;
import com.sampoom.factory.common.exception.BadRequestException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "material_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE material_order SET deleted = true, deleted_at = now() WHERE material_order_id = ?")
@Where(clause = "deleted = false")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

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
