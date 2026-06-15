package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.domain.exception.UserNotFoundException;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.out.EmailVerificationPort;
import com.bookplus.auth.domain.port.out.LoadUserPort;
import com.bookplus.auth.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Verificación de correo.
 *  - POST /api/v1/auth/verify-email           (público) → valida el token
 *  - POST /api/v1/auth/me/resend-verification (auth)    → reenvía el correo
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Email Verification", description = "Verify account email")
public class VerificationController {

    private final EmailVerificationPort emailVerification;
    private final LoadUserPort          loadUserPort;

    @PostMapping("/verify-email")
    @Operation(summary = "Verify the account email with a token")
    public ResponseEntity<ApiResponse<Void>> verify(@RequestBody VerifyEmailRequest body) {
        emailVerification.verifyToken(body.token());
        return ResponseEntity.ok(ApiResponse.ok("Correo verificado"));
    }

    @PostMapping("/me/resend-verification")
    @Operation(summary = "Resend the verification email to the current user")
    public ResponseEntity<ApiResponse<Void>> resend(Authentication auth) {
        User user = loadUserPort.findById(UserId.of(auth.getName()))
                .orElseThrow(() -> new UserNotFoundException(auth.getName()));
        if (!user.isEmailVerified()) {
            emailVerification.createAndSendVerification(
                    user.getId().value(), user.getEmail().value(), user.getUsername());
        }
        return ResponseEntity.ok(ApiResponse.ok("Correo de verificación reenviado"));
    }

    public record VerifyEmailRequest(@NotBlank String token) {}
}
