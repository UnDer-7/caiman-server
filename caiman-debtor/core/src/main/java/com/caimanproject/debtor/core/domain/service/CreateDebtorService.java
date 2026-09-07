package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.contracts.exception.BusinessException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.domain.types.BusinessExceptionCode;
import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class CreateDebtorService implements CreateDebtorUseCase {

    private final DebtorPersistenceGateway debtorPersistenceGateway;

    @Override
    public Debtor execute(final CreateDebtorCommand command) {
        log.info(
                LogField.Placeholders.THREE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "creating debtor"),
                StructuredArguments.kv(LogField.DEBTOR_NAME.label(), command.name()),
                StructuredArguments.kv(
                        LogField.CONTACTS_COUNT.label(), command.contacts().size()));

        final var contacts = command.contacts().stream()
                .map(contact -> DebtorContact.createBuilder()
                        .contactType(contact.contactType())
                        .contactValue(contact.contactValue())
                        .priority(contact.priority())
                        .build())
                .toList();
        validateContacts(contacts);

        final var debtor = Debtor.createBuilder()
                .name(command.name())
                .notes(command.notes())
                .notificationsEnabled(command.notificationsEnabled())
                .contacts(contacts)
                .build();

        final var saved = debtorPersistenceGateway.save(debtor);

        log.info(
                LogField.Placeholders.FOUR.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "debtor created"),
                StructuredArguments.kv(LogField.DEBTOR_ID.label(), saved.getId().orElseThrow()),
                StructuredArguments.kv(LogField.DEBTOR_NAME.label(), command.name()),
                StructuredArguments.kv(
                        LogField.CONTACTS_COUNT.label(), command.contacts().size()));

        return saved;
    }

    private static void validateContacts(final List<DebtorContact> contacts) {
        final var priorityValidation = Debtor.validateDuplicateContactsByPriority(
                contacts, BusinessExceptionCode.DUPLICATE_CONTACT_BY_PRIORITY);
        final var valueValidation =
                Debtor.validateDuplicateContactsByValue(contacts, BusinessExceptionCode.DUPLICATE_CONTACT_BY_VALUE);

        priorityValidation.merge(valueValidation).throwIfInvalid(BusinessException::new);
    }
}
