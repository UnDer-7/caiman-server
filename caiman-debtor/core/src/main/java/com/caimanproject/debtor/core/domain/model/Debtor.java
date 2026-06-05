package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.util.DomainValidation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@ToString
public class Debtor {

    @Getter(AccessLevel.NONE)
    // todo: usar tipo UUID em todos IDs
    private final String id;

    private final String name;

    @Getter(AccessLevel.NONE)
    private final String notes;

    private final Boolean notificationsEnabled;

    private final Boolean active;

    private final Audit audit;

    private final List<DebtorContact> contacts;

    @Builder
    public Debtor(final String id, final String name, final String notes, final Boolean notificationsEnabled, final Boolean active,
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

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

}
