package com.caimanproject.notification.core.domain.service;

import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.gateway.debtor.DebtorContactSnapshotDto;
import com.caimanproject.contracts.gateway.debtor.DebtorGateway;
import com.caimanproject.contracts.gateway.debtor.DebtorSnapshotDto;
import com.caimanproject.contracts.util.LogMask;
import com.caimanproject.notification.core.domain.model.NotificationOutbox;
import com.caimanproject.notification.core.domain.types.NotificationChannel;
import com.caimanproject.notification.core.domain.types.TriggerType;
import com.caimanproject.notification.core.port.in.CreateInvoiceCreatedOutboxUseCase;
import com.caimanproject.notification.core.port.in.command.CreateInvoiceCreatedOutboxCommand;
import com.caimanproject.notification.core.port.out.NotificationOutboxPersistenceGateway;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateInvoiceCreatedOutboxService implements CreateInvoiceCreatedOutboxUseCase {

    private final DebtorGateway debtorGateway;
    private final NotificationOutboxPersistenceGateway notificationOutboxPersistenceGateway;

    @Override
    public void execute(final CreateInvoiceCreatedOutboxCommand command) {
        if (Boolean.FALSE.equals(command.invoiceCreatedNotificationEnabled())) {
            log.info(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(),
                            "INVOICE_CREATED notification disabled (plan or trigger config), skipping outbox"),
                    StructuredArguments.kv(LogField.CHARGE_PLAN_NAME.label(), command.planName()),
                    StructuredArguments.kv(LogField.INVOICE_ID.label(), command.invoiceId()));
            return;
        }

        final Optional<DebtorSnapshotDto> debtor = debtorGateway.findById(command.debtorId());
        if (debtor.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "debtor not found, skipping INVOICE_CREATED outbox"),
                    StructuredArguments.kv(LogField.DEBTOR_ID.label(), command.debtorId()),
                    StructuredArguments.kv(LogField.INVOICE_ID.label(), command.invoiceId()));
            return;
        }

        if (Boolean.FALSE.equals(debtor.get().notificationsEnabled())) {
            log.info(
                    LogField.Placeholders.FOUR.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "notifications disabled for debtor, skipping INVOICE_CREATED outbox"),
                    StructuredArguments.kv(LogField.DEBTOR_ID.label(), command.debtorId()),
                    StructuredArguments.kv(LogField.INVOICE_ID.label(), command.invoiceId()),
                    StructuredArguments.kv(
                            LogField.DEBTOR_NOTIFICATIONS_ENABLED.label(),
                            debtor.get().notificationsEnabled()));
            return;
        }

        final Map<String, String> recipientByContactType = resolveRecipientsByContactType(debtor.get());
        if (recipientByContactType.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "debtor has no contact on file, skipping INVOICE_CREATED outbox"),
                    StructuredArguments.kv(LogField.DEBTOR_ID.label(), command.debtorId()),
                    StructuredArguments.kv(LogField.INVOICE_ID.label(), command.invoiceId()));
            return;
        }

        recipientByContactType.forEach(
                (contactType, recipient) -> createOutboxForChannel(command, debtor.get(), contactType, recipient));
    }

    private void createOutboxForChannel(
            final CreateInvoiceCreatedOutboxCommand command,
            final DebtorSnapshotDto debtor,
            final String contactType,
            final String recipient) {
        final Optional<NotificationChannel> channel = toSupportedChannel(contactType);
        if (channel.isEmpty()) {
            log.warn(
                    LogField.Placeholders.THREE.getPlaceholder(),
                    StructuredArguments.kv(
                            LogField.MSG.label(), "no dispatcher implemented for contact type, skipping"),
                    StructuredArguments.kv(LogField.CHANNEL.label(), contactType),
                    StructuredArguments.kv(LogField.DEBTOR_ID.label(), command.debtorId()));
            return;
        }

        final NotificationOutbox outbox = NotificationOutbox.createBuilder()
                .invoiceId(command.invoiceId())
                .triggerType(TriggerType.INVOICE_CREATED)
                .channel(channel.get())
                .recipient(recipient)
                .debtorName(debtor.name())
                .planName(command.planName())
                .amountDue(command.amountDue())
                .dueDate(command.dueDate())
                .uploadLink(command.uploadLink())
                .cycleIndex(command.cycleIndex())
                .scheduledFor(command.scheduledFor())
                .maxAttempts(command.maxAttempts())
                .build();

        notificationOutboxPersistenceGateway.save(outbox);

        log.info(
                LogField.Placeholders.FIVE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "INVOICE_CREATED notification outbox entry created"),
                StructuredArguments.kv(LogField.INVOICE_ID.label(), command.invoiceId()),
                StructuredArguments.kv(LogField.CHANNEL.label(), channel.get()),
                StructuredArguments.kv(LogField.RECIPIENT.label(), LogMask.email(recipient)),
                StructuredArguments.kv(LogField.DEBTOR_ID.label(), command.debtorId()));
    }

    /**
     * Groups contacts by type and picks the lowest-priority (highest precedence) contact within each group — one
     * recipient per distinct contact type, never one per raw contact row.
     */
    private static Map<String, String> resolveRecipientsByContactType(final DebtorSnapshotDto debtor) {
        return debtor.contacts().stream()
                .collect(Collectors.groupingBy(
                        DebtorContactSnapshotDto::contactType,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(DebtorContactSnapshotDto::priority)),
                                contact -> contact.map(DebtorContactSnapshotDto::contactValue)
                                        .orElseThrow())));
    }

    private static Optional<NotificationChannel> toSupportedChannel(final String contactType) {
        try {
            return Optional.of(NotificationChannel.valueOf(contactType));
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
