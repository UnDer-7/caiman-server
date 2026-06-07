package com.caimanproject.debtor.core.domain.service;

import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.core.port.in.command.CreateDebtorCommand;
import com.caimanproject.debtor.core.port.out.DebtorPersistenceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        final var debtor = Debtor.createBuilder()
            .name(command.name())
            .notes(command.notes())
            .notificationsEnabled(command.notificationsEnabled())
            .contacts(contacts)
            .build();

        return debtorPersistenceGateway.save(debtor);
    }

}
