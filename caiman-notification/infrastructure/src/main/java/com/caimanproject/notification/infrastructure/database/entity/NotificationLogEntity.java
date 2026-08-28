package com.caimanproject.notification.infrastructure.database.entity;

import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.NotificationLogStatus;
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
        name = "notification_log",
        indexes = {@Index(name = "idx_log_invoice", columnList = "invoice_id")})
public class NotificationLogEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @Column(name = "outbox_id", length = 36, nullable = false)
    private String outboxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50, nullable = false)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 50, nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private NotificationLogStatus status;

    @Column(name = "recipient", length = 255, nullable = false)
    private String recipient;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Embedded
    private AuditEmbeddable audit;
}
