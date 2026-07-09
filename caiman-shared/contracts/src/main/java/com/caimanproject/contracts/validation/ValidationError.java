package com.caimanproject.contracts.validation;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
@Builder
@ToString
public class ValidationError {

    private final ExceptionCode code;

    @Getter(AccessLevel.NONE)
    private final ValidationErrorSource source;

    @Getter(AccessLevel.NONE)
    private final String detail;

    public ValidationError(final ExceptionCode code, final ValidationErrorSource source, final String detail) {
        this.code = code;
        this.source = source;
        this.detail = detail;
    }

    public Optional<String> getDetail() {
        return Optional.ofNullable(detail).filter(Predicate.not(String::isBlank));
    }

    public Optional<ValidationErrorSource> getSource() {
        return Optional.ofNullable(source);
    }

}
