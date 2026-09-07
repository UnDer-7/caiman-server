package com.caimanproject.billing.infrastructure.database.entity;

import com.caimanproject.billing.core.domain.types.ChargePlanStatus;
import com.caimanproject.billing.core.domain.types.ChargePlanType;
import com.caimanproject.billing.core.domain.types.CycleUnit;
import com.caimanproject.billing.core.domain.types.ProofValidationMode;
import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
@Table(name = "charge_plan")
public class ChargePlanEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private ChargePlanType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ChargePlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_validation_mode", length = 50, nullable = false)
    private ProofValidationMode proofValidationMode;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "due_tolerance_days", nullable = false)
    private Integer dueToleranceDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_unit", length = 20, nullable = false)
    private CycleUnit cycleUnit;

    @Column(name = "cycle_interval", nullable = false)
    private Integer cycleInterval;

    @Column(name = "cycle_anchor_date", nullable = false)
    private LocalDate cycleAnchorDate;

    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled;

    @Column(name = "notification_time", nullable = false)
    private LocalTime notificationTime;

    @Column(name = "notification_timezone", length = 100, nullable = false)
    private String notificationTimezone;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "end_when_recovered", precision = 15, scale = 2)
    private BigDecimal endWhenRecovered;

    @Embedded
    private AuditEmbeddable audit;

    @Builder.Default
    @OneToMany(mappedBy = "chargePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChargePlanNotificationConfigEntity> notificationConfigs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "chargePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChargePlanMemberEntity> members = new ArrayList<>();

    public void addNotificationConfig(final ChargePlanNotificationConfigEntity config) {
        if (config != null) {
            config.setChargePlan(this);
            notificationConfigs.add(config);
        }
    }

    public void addNotificationConfigs(final Collection<ChargePlanNotificationConfigEntity> configs) {
        if (configs != null && !configs.isEmpty()) {
            configs.forEach(this::addNotificationConfig);
        }
    }

    public void addMember(final ChargePlanMemberEntity member) {
        if (member != null) {
            member.setChargePlan(this);
            members.add(member);
        }
    }

    public void addMembers(final Collection<ChargePlanMemberEntity> members) {
        if (members != null && !members.isEmpty()) {
            members.forEach(this::addMember);
        }
    }
}
