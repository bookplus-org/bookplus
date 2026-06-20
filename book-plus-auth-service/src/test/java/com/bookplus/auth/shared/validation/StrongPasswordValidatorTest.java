package com.bookplus.auth.shared.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el validador de fortaleza de contraseña (sin Spring).
 */
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    private boolean valid(String password) {
        return validator.isValid(password, null);
    }

    @Test
    void acepta_una_contrasena_fuerte() {
        assertThat(valid("Abcdef1!")).isTrue();
        assertThat(valid("S3gur@-2026")).isTrue();
    }

    @Test
    void rechaza_las_debiles() {
        assertThat(valid("abcdef1!")).isFalse();  // sin mayúscula
        assertThat(valid("ABCDEF1!")).isFalse();  // sin minúscula
        assertThat(valid("Abcdefg!")).isFalse();  // sin dígito
        assertThat(valid("Abcdefg1")).isFalse();  // sin símbolo
        assertThat(valid("Ab1!")).isFalse();       // demasiado corta
    }

    @Test
    void null_o_vacio_lo_gestiona_NotBlank() {
        assertThat(valid(null)).isTrue();
        assertThat(valid("")).isTrue();
    }
}
