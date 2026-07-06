package com.bookplus.auth.application.webauthn;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba del verificador WebAuthn con un par de claves EC P-256 real: se fabrica
 * una aserción (clientDataJSON + authenticatorData) y se firma como lo haría un
 * autenticador; luego se comprueba que el verificador la acepta y que rechaza las
 * manipulaciones (reto, origin o firma alterados).
 */
class WebAuthnAssertionVerifierTest {

    private final WebAuthnAssertionVerifier verifier = new WebAuthnAssertionVerifier();

    private static final String RP_ID = "bookplus.example";
    private static final String ORIGIN = "https://bookplus.example";

    @Test
    void aceptaUnaAsercionValida() throws Exception {
        Fixture f = buildSignedAssertion();

        boolean ok = verifier.verify(f.keyPair.getPublic(), f.authData, f.clientData,
                f.signature, f.challengeB64, ORIGIN, RP_ID);

        assertThat(ok).isTrue();
    }

    @Test
    void rechazaRetoIncorrecto() throws Exception {
        Fixture f = buildSignedAssertion();

        boolean ok = verifier.verify(f.keyPair.getPublic(), f.authData, f.clientData,
                f.signature, "reto-que-no-es", ORIGIN, RP_ID);

        assertThat(ok).isFalse();
    }

    @Test
    void rechazaOriginIncorrecto() throws Exception {
        Fixture f = buildSignedAssertion();

        boolean ok = verifier.verify(f.keyPair.getPublic(), f.authData, f.clientData,
                f.signature, f.challengeB64, "https://phishing.example", RP_ID);

        assertThat(ok).isFalse();
    }

    @Test
    void rechazaFirmaManipulada() throws Exception {
        Fixture f = buildSignedAssertion();
        byte[] tampered = f.signature.clone();
        tampered[tampered.length - 1] ^= 0x01; // altera un bit

        boolean ok = verifier.verify(f.keyPair.getPublic(), f.authData, f.clientData,
                tampered, f.challengeB64, ORIGIN, RP_ID);

        assertThat(ok).isFalse();
    }

    // ── Utilidades de prueba: fabrican y firman una aserción como un autenticador ──

    private record Fixture(KeyPair keyPair, byte[] authData, byte[] clientData,
                           byte[] signature, String challengeB64) {}

    private Fixture buildSignedAssertion() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        String challengeB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);

        String clientJson = "{\"type\":\"webauthn.get\",\"challenge\":\"" + challengeB64
                + "\",\"origin\":\"" + ORIGIN + "\"}";
        byte[] clientData = clientJson.getBytes(StandardCharsets.UTF_8);

        byte[] rpIdHash = sha256(RP_ID.getBytes(StandardCharsets.UTF_8));
        byte[] authData = new byte[37];              // 32 rpIdHash + 1 flags + 4 signCount
        System.arraycopy(rpIdHash, 0, authData, 0, 32);
        authData[32] = 0x01;                          // User Present

        byte[] signedData = concat(authData, sha256(clientData));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(kp.getPrivate());
        signer.update(signedData);
        byte[] signature = signer.sign();

        return new Fixture(kp, authData, clientData, signature, challengeB64);
    }

    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
