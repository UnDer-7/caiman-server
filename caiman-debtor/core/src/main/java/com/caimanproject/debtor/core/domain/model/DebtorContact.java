package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.debtor.core.domain.exception.domain.DebtorDomainExceptionCode;
import com.caimanproject.debtor.core.domain.types.ContactType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@ToString
public class DebtorContact {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final ContactType contactType;

    private final String contactValue;

    private final Integer priority;

    private final Audit audit;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public DebtorContact(final UUID id, final ContactType contactType, final String contactValue, final Integer priority, final Audit audit) {

        this.id = id;
        this.contactType = validateOrThrows(contactType, "contactType");
        this.contactValue = validateOrThrows(contactValue, "contactValue");
        this.priority = validateOrThrows(priority, "priority");
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public DebtorContact(final ContactType contactType, final String contactValue, final Integer priority) {
        this(null, contactType, contactValue, priority, null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    private static <T> T validateOrThrows(final T value, final String valueName) {
        return DomainValidation.validateOrThrows(value, valueName, DebtorDomainExceptionCode.DOMAIN_INVALID_VALUE::createException);
    }
}
