package com.caimanproject.debtor.core.test.builder;

import com.caimanproject.debtor.core.domain.model.Audit;
import com.caimanproject.debtor.core.domain.model.DebtorContact;
import com.caimanproject.debtor.core.domain.types.ContactType;

import java.time.Instant;
import java.util.UUID;

public final class DomainBuilder {

    private DomainBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static Audit.AuditBuilder buildAuditFull() {
        return Audit.builder()
            .createdAt(Instant.now())
            .updatedAt(Instant.now());
    }

    public static DebtorContact.RestoreBuilder buildDebtorContactFull() {
        return DebtorContact.restoreBuilder()
            .id(UUID.randomUUID())
            .contactType(ContactType.EMAIL)
            .contactValue("johndoe@gmail.com")
            .priority(1)
            .audit(buildAuditFull().build());
    }
}
