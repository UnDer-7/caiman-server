package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.util.DomainValidation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Getter
@ToString
public class Debtor {

    @Getter(AccessLevel.NONE)
    private final UUID id;

    private final String name;

    @Getter(AccessLevel.NONE)
    private final String notes;

    private final Boolean notificationsEnabled;

    private final Boolean active;

    private final Audit audit;

    private final List<DebtorContact> contacts;

    @Builder(builderMethodName = "restoreBuilder", builderClassName = "RestoreBuilder")
    public Debtor(final UUID id, final String name, final String notes, final Boolean notificationsEnabled, final Boolean active,
        final List<DebtorContact> contacts, final Audit audit) {

        this.id = id;
        this.name = DomainValidation.validateAndReturn(name, "name");
        this.notes = notes;
        this.notificationsEnabled = DomainValidation.validateAndReturn(notificationsEnabled, "notificationsEnabled");
        this.active = DomainValidation.validateAndReturn(active, "active");
        this.contacts = Optional.ofNullable(contacts)
            .filter(Predicate.not(List::isEmpty))
            .map(List::copyOf)
            .orElseGet(Collections::emptyList);
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public Debtor(final String name, final String notes, final Boolean notificationsEnabled, final List<DebtorContact> contacts) {
        this(null, name, notes, notificationsEnabled, true, contacts, null);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

}
