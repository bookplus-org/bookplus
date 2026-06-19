package com.bookplus.order.shared.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Proveedor de la clave AES para el cifrado de PII en reposo.
 *
 * La clave (32 bytes, Base64) se lee de la propiedad de sistema o variable de entorno
 * {@code PII_ENCRYPTION_KEY} — en producción la sirve Vault. Si no está, cae a una clave de
 * DESARROLLO (solo para tests/local; nunca usar en producción). Se carga de forma perezosa
 * para permitir fijar la propiedad antes del primer uso.
 */
public final class PiiCryptoKey {

    // Clave de desarrollo (32 bytes Base64). NO usar en producción.
    private static final String DEV_KEY = "Ym9va3BsdXMtZGV2LWFlczI1Ni1rZXktMzJieXRlcyE=";

    private static volatile SecretKey key;

    private PiiCryptoKey() {}

    public static SecretKey get() {
        SecretKey k = key;
        if (k == null) {
            synchronized (PiiCryptoKey.class) {
                k = key;
                if (k == null) {
                    k = load();
                    key = k;
                }
            }
        }
        return k;
    }

    private static SecretKey load() {
        String b64 = System.getProperty("PII_ENCRYPTION_KEY");
        if (b64 == null || b64.isBlank()) {
            b64 = System.getenv("PII_ENCRYPTION_KEY");
        }
        if (b64 == null || b64.isBlank()) {
            b64 = DEV_KEY; // fallback de desarrollo
        }
        byte[] raw = Base64.getDecoder().decode(b64);
        return new SecretKeySpec(raw, "AES");
    }
}
