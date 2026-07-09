package com.caimanproject.web.annotation.composition.body;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Positive;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Positive(payload = BodyParam.class) @Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PositiveBody {
    // required by Bean Validation spec, but unused — no @ReportAsSingleViolation, actual message comes from the
    // composed @Positive above
    String message() default "must be positive";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
