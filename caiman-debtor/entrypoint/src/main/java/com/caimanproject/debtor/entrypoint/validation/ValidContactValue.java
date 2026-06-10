package com.caimanproject.debtor.entrypoint.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ContactValueValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContactValue {
    String message() default "invalid contact value for the given contact type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
