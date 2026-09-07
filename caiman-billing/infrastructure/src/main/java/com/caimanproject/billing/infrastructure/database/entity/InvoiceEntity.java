package com.caimanproject.billing.infrastructure.database.entity;

import com.caimanproject.billing.core.domain.types.InvoiceStatus;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(
        name = "invoice",
        indexes = {
            @Index(name = "idx_invoice_status_due_date", columnList = "status, due_date"),
            @Index(name = "idx_invoice_charge_plan_member", columnList = "charge_plan_member_id"),
            @Index(name = "idx_invoice_charge_plan", columnList = "charge_plan_id"),
            @Index(name = "uq_invoice_upload_token", columnList = "upload_token", unique = true)
        })
public class InvoiceEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charge_plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_invoice_charge_plan"))
    private ChargePlanEntity chargePlan;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "charge_plan_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_invoice_charge_plan_member"))
    private ChargePlanMemberEntity chargePlanMember;

    @Column(name = "cycle_index", nullable = false)
    private Long cycleIndex;

    @Column(name = "generation_date", nullable = false)
    private LocalDate generationDate;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private InvoiceStatus status;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "upload_token", length = 36, nullable = false)
    private String uploadToken;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Embedded
    private AuditEmbeddable audit;
}
