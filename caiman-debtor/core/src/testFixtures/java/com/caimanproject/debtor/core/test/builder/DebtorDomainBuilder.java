package com.caimanproject.debtor.core.test.builder;

import com.caimanproject.debtor.core.domain.model.Audit;
import com.caimanproject.debtor.core.domain.model.Debtor;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.domain.types.ContactType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DebtorDomainBuilder {

    private DebtorDomainBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static Audit.AuditBuilder buildAuditFull() {
        return Audit.builder().createdAt(Instant.now()).updatedAt(Instant.now());
    }

    public static DebtorContact.RestoreBuilder buildDebtorContactFull() {
        return DebtorContact.restoreBuilder()
                .id(UUID.randomUUID())
                .contactType(ContactType.EMAIL)
                .contactValue("johndoe@gmail.com")
                .priority(1)
                .audit(buildAuditFull().build());
    }

    public static Debtor.RestoreBuilder buildDebtorFull() {
        return Debtor.restoreBuilder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .notes("Lorem ipsum")
                .notificationsEnabled(true)
                .active(true)
                .contacts(List.of(buildDebtorContactFull().build()))
                .audit(buildAuditFull().build());
    }
}
