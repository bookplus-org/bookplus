package com.bookplus.auth.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Configuración del par de claves RSA para JWT RS256.
 *
 * En desarrollo, si no se configuran claves, se genera un par efímero automáticamente.
 * En producción, configurar via variables de entorno:
 *   APP_JWT_PRIVATE_KEY=<base64>
 *   APP_JWT_PUBLIC_KEY=<base64>
 *
 * Para generar claves de producción:
 *   openssl genrsa -out private.pem 2048
 *   openssl rsa -in private.pem -pubout -out public.pem
 *   cat private.pem | base64 -w 0  → APP_JWT_PRIVATE_KEY
 *   cat public.pem | base64 -w 0   → APP_JWT_PUBLIC_KEY
 */
@Configuration
@Slf4j
public class JwtConfig {

    @Bean
    public JwtProperties jwtProperties(RsaKeyProperties rsaKeyProperties) throws Exception {
        KeyPair keyPair = resolveKeyPair(rsaKeyProperties);

        return new JwtProperties(
                (PrivateKey) keyPair.getPrivate(),
                (PublicKey)  keyPair.getPublic(),
                rsaKeyProperties.accessTokenExpiration(),
                rsaKeyProperties.refreshTokenExpiration()
        );
    }

    private KeyPair resolveKeyPair(RsaKeyProperties props) throws Exception {
        if (props.privateKey() != null && !props.privateKey().isBlank()
                && props.publicKey() != null && !props.publicKey().isBlank()) {
            return loadKeyPairFromBase64(props.privateKey(), props.publicKey());
        }
        log.warn("⚠️  No RSA keys configured — generating ephemeral keypair for DEVELOPMENT ONLY");
        return generateEphemeralKeyPair();
    }

    private KeyPair loadKeyPairFromBase64(String privateKeyB64, String publicKeyB64) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        byte[] privateBytes = Base64.getMimeDecoder().decode(
                privateKeyB64.replace("-----BEGIN PRIVATE KEY-----", "")
                             .replace("-----END PRIVATE KEY-----", "")
                             .replaceAll("\\s", ""));
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

        byte[] publicBytes = Base64.getMimeDecoder().decode(
                publicKeyB64.replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", ""));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));

        return new KeyPair(publicKey, privateKey);
    }

    private KeyPair generateEphemeralKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // ── Properties ────────────────────────────────────────────────────────

    @ConfigurationProperties(prefix = "application.security.jwt")
    public record RsaKeyProperties(
            String privateKey,
            String publicKey,
            long   accessTokenExpiration,
            long   refreshTokenExpiration
    ) {
        @ConstructorBinding
        public RsaKeyProperties(String privateKey, String publicKey,
                                long accessTokenExpiration, long refreshTokenExpiration) {
            this.privateKey              = privateKey;
            this.publicKey               = publicKey;
            this.accessTokenExpiration   = accessTokenExpiration > 0 ? accessTokenExpiration : 900_000L;
            this.refreshTokenExpiration  = refreshTokenExpiration > 0 ? refreshTokenExpiration : 604_800_000L;
        }
    }

    public record JwtProperties(
            PrivateKey privateKey,
            PublicKey  publicKey,
            long       accessTokenExpiration,
            long       refreshTokenExpiration
    ) {}
}
