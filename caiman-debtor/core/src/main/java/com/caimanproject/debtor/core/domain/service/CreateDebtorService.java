package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.debtor.core.domain.exception.business.DebtorBusinessExceptionCode;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class CreateDebtorService implements CreateDebtorUseCase {

    private final DebtorPersistenceGateway debtorPersistenceGateway;

    @Override
    public Debtor execute(final CreateDebtorCommand command) {
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

        return debtorPersistenceGateway.save(debtor);
    }

    private static void validateContacts(final List<DebtorContact> contacts) {
        final List<DebtorContact> duplicateContactsByPriority = Debtor.getDuplicateContactsByPriority(contacts);
        if (!duplicateContactsByPriority.isEmpty()) {
            final var msg = duplicateContactsByPriority.stream()
                .map(dc -> "contactType: %s - contactValue: %s - priority: %s".formatted(
                    dc.getContactType(), dc.getContactValue(), dc.getPriority()))
                .collect(Collectors.joining(" | "));
            throw DebtorBusinessExceptionCode.DUPLICATE_CONTACT_BY_PRIORITY.createException("Duplicate Contacts: " + msg);
        }

        final List<DebtorContact> duplicateContactsByValue = Debtor.getDuplicateContactsByValue(contacts);
        if (!duplicateContactsByValue.isEmpty()) {
            final var msg = duplicateContactsByValue.stream()
                .map(dc -> "contactType: %s - contactValue: %s - priority: %s".formatted(
                    dc.getContactType(), dc.getContactValue(), dc.getPriority()))
                .collect(Collectors.joining(" | "));
            throw DebtorBusinessExceptionCode.DUPLICATE_CONTACT_BY_VALUE.createException("Duplicate Contacts: " + msg);
        }
    }

}
