package com.bookplus.auth.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida la fortaleza de una contraseña: mínimo 8 caracteres y al menos una mayúscula, una
 * minúscula, un dígito y un símbolo (carácter no alfanumérico).
 *
 * El valor nulo/vacío se considera válido aquí: de eso se encarga {@code @NotBlank}, para no
 * duplicar mensajes de error.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // lo gestiona @NotBlank
        }
        if (value.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c))      hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c))     hasDigit = true;
            else                               hasSpecial = true; // cualquier no alfanumérico
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
