package com.caimanproject.notification.infrastructure.database.entity;

import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationOutboxStatus;
import com.caimanproject.notification.core.domain.types.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
@Table(
        name = "notification_outbox",
        indexes = {
            @Index(name = "idx_outbox_scheduled", columnList = "status, scheduled_for"),
            @Index(name = "idx_outbox_invoice_trigger", columnList = "invoice_id, trigger_type, status")
        })
public class NotificationOutboxEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50, nullable = false)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 50, nullable = false)
    private NotificationChannel channel;

    @Column(name = "recipient", length = 255, nullable = false)
    private String recipient;

    @Column(name = "debtor_name", length = 255, nullable = false)
    private String debtorName;

    @Column(name = "plan_name", length = 255, nullable = false)
    private String planName;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "upload_link", length = 1000)
    private String uploadLink;

    @Column(name = "cycle_index")
    private Long cycleIndex;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private NotificationOutboxStatus status;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Embedded
    private AuditEmbeddable audit;
}
