package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.core.domain.types.DomainExceptionCode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
    public DebtorContact(
            final UUID id,
            final ContactType contactType,
            final String contactValue,
            final Integer priority,
            final Audit audit) {

        // Optional
        this.id = id;

        // Required
        this.contactType = contactType;
        this.contactValue = contactValue;
        this.priority = priority;
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldValidations = DomainValidation.validateAll(List.of(
                DomainValidation.validate(contactType, "$.contactType", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(contactValue, "$.contactValue", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(priority, "$.priority", DomainExceptionCode.INVALID_VALUE)));

        fieldValidations.throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public DebtorContact(final ContactType contactType, final String contactValue, final Integer priority) {
        this(null, contactType, contactValue, priority, null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }
}
