package com.sampoom.factory.api.part.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "part_order_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PartOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_order_id", nullable = false)
    private Long partOrderId;

    @Column(name = "factory_id", nullable = false)
    private Long factoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private PartOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private PartOrderStatus toStatus;

    @Column(name = "changed_reason")
    private String changedReason;

    @CreatedDate
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Builder
    public PartOrderHistory(Long partOrderId, Long factoryId, PartOrderStatus fromStatus,
                           PartOrderStatus toStatus, String changedReason) {
        this.partOrderId = partOrderId;
        this.factoryId = factoryId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedReason = changedReason;
    }
}
