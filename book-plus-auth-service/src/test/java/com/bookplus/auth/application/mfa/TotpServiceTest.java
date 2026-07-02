package com.bookplus.auth.application.mfa;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del segundo factor TOTP. Incluye la verificación del núcleo HOTP
 * contra los vectores oficiales del RFC 6238, además del round-trip de Base32,
 * la ventana de tolerancia y el rechazo de códigos inválidos/expirados.
 */
class TotpServiceTest {

    private final TotpService totp = new TotpService();

    // Semilla del RFC 6238 (ASCII "12345678901234567890") en Base32.
    private static final String RFC_SECRET_BASE32 =
            TotpService.base32Encode("12345678901234567890".getBytes());

    @Test
    void base32_roundTrip() {
        byte[] original = "12345678901234567890".getBytes();
        String encoded = TotpService.base32Encode(original);
        assertThat(TotpService.base32Decode(encoded)).isEqualTo(original);
    }

    @Test
    void generateCode_coincideConVectoresRfc6238() {
        // El RFC define códigos de 8 dígitos; la app usa 6 (los 6 finales).
        assertThat(totp.generateCode(RFC_SECRET_BASE32, Instant.ofEpochSecond(59)))
                .isEqualTo("287082");                                   // de 94287082
        assertThat(totp.generateCode(RFC_SECRET_BASE32, Instant.ofEpochSecond(1111111109)))
                .isEqualTo("081804");                                   // de 07081804
        assertThat(totp.generateCode(RFC_SECRET_BASE32, Instant.ofEpochSecond(1234567890)))
                .isEqualTo("005924");                                   // de 89005924
    }

    @Test
    void verify_aceptaElCodigoActual() {
        String secret = totp.generateSecret();
        Instant now = Instant.now();
        String code = totp.generateCode(secret, now);
        assertThat(totp.verify(secret, code, now)).isTrue();
    }

    @Test
    void verify_toleraDesfaseDeUnPaso() {
        String secret = totp.generateSecret();
        Instant now = Instant.ofEpochSecond(1_000_000_000L);
        // Código válido del paso anterior (hace 30s) debe seguir aceptándose.
        String prevCode = totp.generateCode(secret, now.minusSeconds(30));
        assertThat(totp.verify(secret, prevCode, now)).isTrue();
    }

    @Test
    void verify_rechazaCodigoExpirado() {
        String secret = totp.generateSecret();
        Instant now = Instant.ofEpochSecond(1_000_000_000L);
        // Código de hace 5 minutos: fuera de la ventana -> rechazado.
        String oldCode = totp.generateCode(secret, now.minusSeconds(300));
        assertThat(totp.verify(secret, oldCode, now)).isFalse();
    }

    @Test
    void verify_rechazaCodigoInvalido() {
        String secret = totp.generateSecret();
        assertThat(totp.verify(secret, "000000")).isFalse();
        assertThat(totp.verify(secret, null)).isFalse();
    }

    @Test
    void otpauthUri_tieneFormatoEsperado() {
        String uri = totp.otpauthUri("BookPlus", "alice@bookplus.com", "ABCDEF");
        assertThat(uri).startsWith("otpauth://totp/BookPlus:")
                .contains("secret=ABCDEF")
                .contains("issuer=BookPlus")
                .contains("digits=6")
                .contains("period=30");
    }
}
