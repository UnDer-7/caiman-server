package com.caimanproject.debtor.core.domain.model;

import com.caimanproject.contracts.exception.DomainException;
import com.caimanproject.contracts.exception.ExceptionCode;
import com.caimanproject.contracts.util.DomainValidation;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationResult;
import com.caimanproject.debtor.core.domain.types.DomainExceptionCode;
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

        // Optional
        this.id = id;
        this.notes = notes;

        // Required
        this.name = name;
        this.notificationsEnabled = notificationsEnabled;
        this.active = active;
        this.contacts = Optional.ofNullable(contacts)
                .filter(Predicate.not(List::isEmpty))
                .map(List::copyOf)
                .orElseGet(Collections::emptyList);
        this.audit = Objects.requireNonNullElseGet(audit, Audit::new);

        final var fieldsValidation = DomainValidation.validateAll(List.of(
                DomainValidation.validate(name, "$.name", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(
                        notificationsEnabled, "$.notificationsEnabled", DomainExceptionCode.INVALID_VALUE),
                DomainValidation.validate(active, "$.active", DomainExceptionCode.INVALID_VALUE)));

        final var contactPriorityValidation =
                validateDuplicateContactsByPriority(this.contacts, DomainExceptionCode.DUPLICATE_CONTACT_PRIORITY);
        final var contactValueValidation =
                validateDuplicateContactsByValue(this.contacts, DomainExceptionCode.DUPLICATED_CONTACT_VALUE);

        fieldsValidation
                .merge(contactPriorityValidation)
                .merge(contactValueValidation)
                .throwIfInvalid(DomainException::new);
    }

    @Builder(builderMethodName = "createBuilder", builderClassName = "CreateBuilder")
    public Debtor(
            final String name,
            final String notes,
            final Boolean notificationsEnabled,
            final List<DebtorContact> contacts) {
        this(null, name, notes, notificationsEnabled, true, contacts, null);
    }

    public static ValidationResult validateDuplicateContactsByValue(
            final List<DebtorContact> contacts, final ExceptionCode exceptionCode) {
        final var validations = contacts.stream()
                .collect(
                        Collectors.groupingBy(c -> Map.entry(c.getContactValue().toLowerCase(), c.getContactType())))
                .values()
                .stream()
                .filter(group -> group.size() > 1)
                .map(List::getFirst)
                .map(c -> ValidationError.builder()
                        .code(exceptionCode)
                        .source(new ValidationErrorSourceBody("$.contacts[*].contactValue", c.getContactValue()))
                        .detail("contactType: %s - priority: %s".formatted(c.getContactType(), c.getPriority()))
                        .build())
                .toList();
        return ValidationResult.of(validations);
    }

    public static ValidationResult validateDuplicateContactsByPriority(
            final List<DebtorContact> contacts, final ExceptionCode exceptionCode) {
        final var validations = contacts.stream()
                .collect(Collectors.groupingBy(c -> Map.entry(c.getContactType(), c.getPriority())))
                .values()
                .stream()
                .filter(group -> group.size() > 1)
                .map(List::getFirst)
                .map(c -> ValidationError.builder()
                        .code(exceptionCode)
                        .source(new ValidationErrorSourceBody(
                                "$.contacts[*].priority", c.getPriority().toString()))
                        .detail("contactType: %s - contactValue: %s".formatted(c.getContactType(), c.getContactValue()))
                        .build())
                .toList();
        return ValidationResult.of(validations);
    }

    public Optional<UUID> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }
}
