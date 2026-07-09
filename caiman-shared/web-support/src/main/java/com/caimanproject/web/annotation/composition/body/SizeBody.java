package com.caimanproject.web.annotation.composition.body;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Size(min = 0, max = Integer.MAX_VALUE, payload = BodyParam.class) @Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SizeBody {

    // required by Bean Validation spec, but unused — no @ReportAsSingleViolation, actual message comes from the
    // composed @Size above
    String message() default "size must be between {min} and {max}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @OverridesAttribute(constraint = Size.class, name = "min")
    int min() default 0;

    @OverridesAttribute(constraint = Size.class, name = "max")
    int max() default Integer.MAX_VALUE;
}
