package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.debtor.core.domain.types.ContactType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.Optional;

@Getter
@ToString
public class DebtorContact {

    @Getter(AccessLevel.NONE)
    private final String id;

    private final ContactType contactType;

    private final String contactValue;

    private final Integer priority;

    private final Audit audit;

    @Builder
    public DebtorContact(final String id, final ContactType contactType, final String contactValue, final Integer priority, final Audit audit) {

        this.id = id;
        this.contactType = DomainValidation.validateAndReturn(contactType, "contactType");
        this.contactValue = DomainValidation.validateAndReturn(contactValue, "contactValue");
        this.priority = DomainValidation.validateAndReturn(priority, "priority");
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

}
