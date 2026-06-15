package com.caimanproject.debtor.entrypoint.validation;

import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ContactValueValidator implements ConstraintValidator<ValidContactValue, CreateDebtorContactRequestDto> {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]++(?:\\.[a-zA-Z0-9_+&*-]++)*+@(?:[a-zA-Z0-9-]++\\.)++[a-zA-Z]{2,}+$");

    @Override
    public boolean isValid(final CreateDebtorContactRequestDto dto, final ConstraintValidatorContext context) {
        if (dto == null
                || dto.contactType() == null
                || dto.contactValue() == null
                || dto.contactValue().isBlank()) {
            return true;
        }

        return switch (dto.contactType()) {
            case EMAIL -> validate(EMAIL_PATTERN, dto.contactValue(), "must be a valid EMAIL address", context);
        };
    }

    private static boolean validate(
            final Pattern pattern, final String value, final String message, final ConstraintValidatorContext context) {

        if (pattern.matcher(value).matches()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("contactValue")
                .addConstraintViolation();
        return false;
    }
}
