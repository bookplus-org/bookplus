package com.bookplus.auth.application.mfa;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;

/**
 * Segundo factor de autenticación (MFA) mediante TOTP — RFC 6238 (sobre HOTP,
 * RFC 4226). Compatible con Google Authenticator / Authy / 1Password, etc.
 *
 * Implementación en JDK puro (HMAC-SHA1 vía {@link javax.crypto.Mac}), sin
 * dependencias externas. El secreto se comparte con la app del usuario mediante
 * un código QR (URI otpauth://) durante el enrolamiento; después, en cada login,
 * el usuario introduce el código de 6 dígitos que su app genera y el servidor lo
 * verifica con una pequeña ventana de tolerancia por desfase de reloj.
 */
@Service
public class TotpService {

    private static final int    DIGITS = 6;          // longitud del código
    private static final int    PERIOD = 30;         // segundos por código
    private static final int    WINDOW = 1;          // ±1 paso de tolerancia (±30s)
    private static final String HMAC   = "HmacSHA1";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom random = new SecureRandom();

    // ── Enrolamiento ─────────────────────────────────────────────────────────

    /** Genera un secreto aleatorio de 160 bits codificado en Base32 (RFC 4648). */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * URI otpauth:// para pintar el QR en el enrolamiento. Ejemplo:
     * otpauth://totp/BookPlus:alice@x.com?secret=XXXX&issuer=BookPlus&digits=6&period=30
     */
    public String otpauthUri(String issuer, String account, String base32Secret) {
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD;
    }

    // ── Verificación ─────────────────────────────────────────────────────────

    /** Código actual (para el instante dado); útil en pruebas y para el propio servidor. */
    public String generateCode(String base32Secret, Instant when) {
        long counter = when.getEpochSecond() / PERIOD;
        return hotp(base32Decode(base32Secret), counter);
    }

    /** Verifica un código contra el instante actual, tolerando ±1 paso de reloj. */
    public boolean verify(String base32Secret, String code) {
        return verify(base32Secret, code, Instant.now());
    }

    /** Verifica un código respecto a un instante concreto (facilita el testing). */
    public boolean verify(String base32Secret, String code, Instant when) {
        if (code == null) return false;
        String normalized = code.trim();
        byte[] key = base32Decode(base32Secret);
        long counter = when.getEpochSecond() / PERIOD;
        for (int offset = -WINDOW; offset <= WINDOW; offset++) {
            if (constantTimeEquals(hotp(key, counter + offset), normalized)) {
                return true;
            }
        }
        return false;
    }

    // ── Núcleo HOTP (RFC 4226) — verificado contra los vectores del RFC 6238 ──

    private static String hotp(byte[] key, long counter) {
        try {
            byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            byte[] h = mac.doFinal(msg);
            int off = h[h.length - 1] & 0x0F;
            int bin = ((h[off] & 0x7f) << 24)
                    | ((h[off + 1] & 0xff) << 16)
                    | ((h[off + 2] & 0xff) << 8)
                    | (h[off + 3] & 0xff);
            int otp = bin % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el código TOTP", e);
        }
    }

    // ── Base32 (RFC 4648, sin padding) ───────────────────────────────────────

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt(idx));
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(BASE32_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String s) {
        String clean = s.trim().replace("=", "").toUpperCase();
        int buffer = 0, bitsLeft = 0;
        byte[] out = new byte[clean.length() * 5 / 8];
        int idx = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Carácter Base32 inválido: " + c);
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[idx++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return idx == out.length ? out : Arrays.copyOf(out, idx);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
