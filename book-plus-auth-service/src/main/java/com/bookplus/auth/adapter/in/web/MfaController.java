package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.application.mfa.TotpService;
import com.bookplus.auth.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Segundo factor (MFA) por TOTP. Ambas rutas requieren autenticación
 * (SecurityConfig: anyRequest().authenticated()).
 *
 *  - POST /api/v1/auth/mfa/enroll  → genera un secreto y la URI otpauth:// para
 *    que el usuario la escanee con su app (Google Authenticator, etc.).
 *  - POST /api/v1/auth/mfa/verify  → comprueba un código de 6 dígitos.
 *
 * Nota: la persistencia del secreto por usuario y su exigencia en el login se
 * apoyan en este bloque; el núcleo criptográfico (RFC 6238) vive en TotpService.
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA (TOTP)", description = "Two-factor authentication with TOTP")
public class MfaController {

    private static final String ISSUER = "BookPlus";

    private final TotpService totpService;

    @PostMapping("/enroll")
    @Operation(summary = "Generate a TOTP secret and otpauth URI for the current user")
    public ResponseEntity<ApiResponse<EnrollResponse>> enroll(Authentication auth) {
        String secret = totpService.generateSecret();
        String uri = totpService.otpauthUri(ISSUER, auth.getName(), secret);
        return ResponseEntity.ok(ApiResponse.ok(new EnrollResponse(secret, uri)));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a 6-digit TOTP code against a secret")
    public ResponseEntity<ApiResponse<VerifyResponse>> verify(@RequestBody VerifyRequest body) {
        boolean valid = totpService.verify(body.secret(), body.code());
        return ResponseEntity.ok(ApiResponse.ok(new VerifyResponse(valid)));
    }

    public record EnrollResponse(String secret, String otpauthUri) {}

    public record VerifyRequest(@NotBlank String secret, @NotBlank String code) {}

    public record VerifyResponse(boolean valid) {}
}
