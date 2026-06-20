package com.bookplus.auth.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción de Bean Validation: exige una contraseña fuerte (longitud + complejidad).
 * Se aplica con {@code @StrongPassword} sobre el campo y la valida {@link StrongPasswordValidator}.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default
            "La contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula, número y símbolo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
