package com.caimanproject.billing.infrastructure.database.entity;

import com.caimanproject.billing.core.domain.types.ChargePlanMemberStatus;
import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "charge_plan_member")
public class ChargePlanMemberEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charge_plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cpm_charge_plan"))
    private ChargePlanEntity chargePlan;

    @Column(name = "debtor_id", length = 36, nullable = false)
    private String debtorId;

    @Column(name = "amount_override", precision = 15, scale = 2)
    private BigDecimal amountOverride;

    @Column(name = "rotation_order")
    private Integer rotationOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ChargePlanMemberStatus status;

    @Builder.Default
    @Column(name = "credit_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Embedded
    private AuditEmbeddable audit;
}
