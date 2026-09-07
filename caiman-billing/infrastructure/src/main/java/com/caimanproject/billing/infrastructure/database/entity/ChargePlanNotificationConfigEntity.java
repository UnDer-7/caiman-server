package com.caimanproject.billing.infrastructure.database.entity;

import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.TriggerType;
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
import jakarta.persistence.UniqueConstraint;
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
        name = "charge_plan_notification_config",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_cpnc_plan_trigger",
                    columnNames = {"charge_plan_id", "trigger_type"})
        })
public class ChargePlanNotificationConfigEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charge_plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cpnc_charge_plan"))
    private ChargePlanEntity chargePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50, nullable = false)
    private TriggerType triggerType;

    @Column(name = "reminder_interval")
    private Integer reminderInterval;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_unit", length = 20)
    private CycleUnit reminderUnit;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Embedded
    private AuditEmbeddable audit;
}
