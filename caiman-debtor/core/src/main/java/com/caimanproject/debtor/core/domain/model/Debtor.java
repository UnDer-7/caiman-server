package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.debtor.core.domain.exception.domain.DomainExceptionCode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
    public Debtor(
            final UUID id,
            final String name,
            final String notes,
            final Boolean notificationsEnabled,
            final Boolean active,
            final List<DebtorContact> contacts,
            final Audit audit) {

        final var duplicateContactsByValue = getDuplicateContactsByValue(contacts);
        if (!duplicateContactsByValue.isEmpty()) {
            final var msg = duplicateContactsByValue.stream()
                    .map(dc -> "contactType: %s - contactValue: %s - priority: %s"
                            .formatted(dc.getContactType(), dc.getContactValue(), dc.getPriority()))
                    .collect(Collectors.joining(" | "));

            throw DomainExceptionCode.DUPLICATED_CONTACT_VALUE.createException("Repeated Contacts: " + msg);
        }
        final var duplicateContactsByPriority = getDuplicateContactsByPriority(contacts);
        if (!duplicateContactsByPriority.isEmpty()) {
            final var msg = duplicateContactsByPriority.stream()
                    .map(dc -> "contactType: %s - contactValue: %s - priority: %s"
                            .formatted(dc.getContactType(), dc.getContactValue(), dc.getPriority()))
                    .collect(Collectors.joining(" | "));
            throw DomainExceptionCode.DUPLICATE_CONTACT_PRIORITY.createException("Repeated Contacts: " + msg);
        }

        this.id = id;
        this.name = validateOrThrows(name, "name");
        this.notes = notes;
        this.notificationsEnabled = validateOrThrows(notificationsEnabled, "notificationsEnabled");
        this.active = validateOrThrows(active, "active");
        this.contacts = Optional.ofNullable(contacts)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public Debtor(
            final String name,
            final String notes,
            final Boolean notificationsEnabled,
            final List<DebtorContact> contacts) {
        this(null, name, notes, notificationsEnabled, true, contacts, null);
    }

    public static List<DebtorContact> getDuplicateContactsByValue(final List<DebtorContact> contacts) {
        return contacts.stream()
                .collect(
                        Collectors.groupingBy(c -> Map.entry(c.getContactValue().toLowerCase(), c.getContactType())))
                .values()
                .stream()
                .filter(group -> group.size() > 1)
                .map(List::getFirst)
                .toList();
    }

    public static List<DebtorContact> getDuplicateContactsByPriority(final List<DebtorContact> contacts) {
        return contacts.stream()
                .collect(Collectors.groupingBy(c -> Map.entry(c.getContactType(), c.getPriority())))
                .values()
                .stream()
                .filter(group -> group.size() > 1)
                .map(List::getFirst)
                .toList();
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    private static <T> T validateOrThrows(final T value, final String valueName) {
        return DomainValidation.validateOrThrows(value, valueName, DomainExceptionCode.INVALID_VALUE::createException);
    }
}
