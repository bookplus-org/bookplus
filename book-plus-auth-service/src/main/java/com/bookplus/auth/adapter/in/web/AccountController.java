package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.in.AccountUseCase;
import com.bookplus.auth.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Autogestión de la cuenta del usuario autenticado.
 * Protegido por SecurityConfig: requiere autenticación (no admin).
 */
@RestController
@RequestMapping("/api/v1/auth/me")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Self-service account management")
public class AccountController {

    private final AccountUseCase accountUseCase;

    @PatchMapping("/profile")
    @Operation(summary = "Update my username and email")
    public ResponseEntity<ApiResponse<MeResponse>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest body) {
        User updated = accountUseCase.updateMyProfile(
                UserId.of(auth.getName()), body.username(), body.email());
        return ResponseEntity.ok(ApiResponse.ok(MeResponse.from(updated)));
    }

    @PatchMapping("/password")
    @Operation(summary = "Change my password (requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest body) {
        accountUseCase.changeMyPassword(
                UserId.of(auth.getName()), body.currentPassword(), body.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada"));
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}

    public record MeResponse(String id, String username, String email, List<String> roles) {
        public static MeResponse from(User u) {
            return new MeResponse(
                    u.getId().toString(), u.getUsername(), u.getEmail().value(),
                    u.getRoles().stream().map(Enum::name).sorted().toList());
        }
    }
}
