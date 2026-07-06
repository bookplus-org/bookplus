package com.bookplus.auth.application.webauthn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

/**
 * Verificación del lado servidor de una aserción WebAuthn / FIDO2 (passkeys).
 *
 * WebAuthn es autenticación RESISTENTE A PHISHING: en lugar de una contraseña (o
 * un TOTP que se puede teclear en un sitio falso), el autenticador del usuario
 * (llave de seguridad, Touch ID, Windows Hello) firma un reto con una clave
 * privada que nunca sale del dispositivo, y el servidor verifica la firma con la
 * clave pública registrada. El "origin" va firmado, así que una web falsa no
 * puede reutilizar la firma.
 *
 * Esta clase implementa el núcleo de verificación de la fase de login (assertion):
 *  1) clientDataJSON: type=webauthn.get, challenge y origin esperados;
 *  2) authenticatorData: rpIdHash correcto y flag "User Present";
 *  3) firma ECDSA (P-256) sobre  authenticatorData || SHA-256(clientDataJSON).
 * Todo con JDK puro (java.security), sin librerías externas.
 */
@Service
public class WebAuthnAssertionVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnAssertionVerifier.class);

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param publicKey            clave pública EC (P-256) registrada del credencial
     * @param authenticatorData    bytes de authenticatorData devueltos por el autenticador
     * @param clientDataJson       bytes de clientDataJSON
     * @param signature            firma (DER) sobre authenticatorData||hash(clientData)
     * @param expectedChallengeB64 reto esperado, en base64url (como aparece en clientDataJSON)
     * @param expectedOrigin       origin esperado (p. ej. https://bookplus.example)
     * @param rpId                 Relying Party ID (dominio), p. ej. bookplus.example
     * @return true si la aserción es válida
     */
    public boolean verify(PublicKey publicKey, byte[] authenticatorData, byte[] clientDataJson,
                          byte[] signature, String expectedChallengeB64,
                          String expectedOrigin, String rpId) {
        try {
            // 1) clientDataJSON
            JsonNode cd = mapper.readTree(clientDataJson);
            if (!"webauthn.get".equals(cd.path("type").asText())) return fail("type != webauthn.get");
            if (!expectedChallengeB64.equals(cd.path("challenge").asText())) return fail("challenge no coincide");
            if (!expectedOrigin.equals(cd.path("origin").asText())) return fail("origin no coincide");

            // 2) authenticatorData: rpIdHash (32 bytes) + flags (1) + signCount (4) + ...
            if (authenticatorData.length < 37) return fail("authenticatorData demasiado corto");
            byte[] expectedRpIdHash = sha256(rpId.getBytes(StandardCharsets.UTF_8));
            byte[] actualRpIdHash = Arrays.copyOfRange(authenticatorData, 0, 32);
            if (!MessageDigest.isEqual(expectedRpIdHash, actualRpIdHash)) return fail("rpIdHash no coincide");
            byte flags = authenticatorData[32];
            if ((flags & 0x01) == 0) return fail("flag User Present no activado");

            // 3) firma sobre authenticatorData || SHA-256(clientDataJSON)
            byte[] clientDataHash = sha256(clientDataJson);
            byte[] signedData = concat(authenticatorData, clientDataHash);
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            ecdsa.initVerify(publicKey);
            ecdsa.update(signedData);
            return ecdsa.verify(signature);
        } catch (Exception e) {
            return fail("excepción: " + e.getMessage());
        }
    }

    private boolean fail(String reason) {
        log.debug("Verificación WebAuthn fallida: {}", reason);
        return false;
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
