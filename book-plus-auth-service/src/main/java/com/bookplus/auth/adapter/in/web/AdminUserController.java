package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.model.UserRole;
import com.bookplus.auth.domain.port.in.AdminUserUseCase;
import com.bookplus.auth.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador web — gestión administrativa de usuarios.
 * Protegido en SecurityConfig: /api/v1/admin/** → ROLE_ADMIN o ROLE_SUPERADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Administración de cuentas de usuario")
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> list() {
        List<AdminUserResponse> users = adminUserUseCase.listUsers().stream()
                .map(AdminUserResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping
    @Operation(summary = "Crear una cuenta de usuario")
    public ResponseEntity<ApiResponse<AdminUserResponse>> create(@Valid @RequestBody CreateUserRequest body) {
        User created = adminUserUseCase.createUser(
                body.username(), body.email(), body.password(), parseRole(body.role()));
        return ResponseEntity.ok(ApiResponse.ok("Usuario creado", AdminUserResponse.from(created)));
    }

    @PatchMapping("/{id}/profile")
    @Operation(summary = "Editar nombre de usuario y correo")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest body) {
        User updated = adminUserUseCase.updateProfile(UserId.of(id), body.username(), body.email());
        return ResponseEntity.ok(ApiResponse.ok(AdminUserResponse.from(updated)));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Restablecer la contraseña")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String id,
            @Valid @RequestBody ResetPasswordRequest body) {
        adminUserUseCase.resetPassword(UserId.of(id), body.password());
        return ResponseEntity.ok(ApiResponse.ok("Contraseña restablecida"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar o desactivar una cuenta")
    public ResponseEntity<ApiResponse<AdminUserResponse>> setStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest body) {
        User updated = adminUserUseCase.setEnabled(UserId.of(id), body.enabled(), body.reason());
        return ResponseEntity.ok(ApiResponse.ok(AdminUserResponse.from(updated)));
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Otorgar o revocar un rol")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @PathVariable String id,
            @RequestBody ChangeRoleRequest body) {
        UserRole role = parseRole(body.role());
        User updated = adminUserUseCase.changeRole(UserId.of(id), role, body.grant());
        return ResponseEntity.ok(ApiResponse.ok(AdminUserResponse.from(updated)));
    }

    private static UserRole parseRole(String raw) {
        String name = raw == null ? "" : raw.trim().toUpperCase();
        if (!name.startsWith("ROLE_")) {
            name = "ROLE_" + name;
        }
        return UserRole.valueOf(name);
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    public record UpdateStatusRequest(boolean enabled, String reason) {}

    public record ChangeRoleRequest(@NotBlank String role, boolean grant) {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String role
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record AdminUserResponse(
            String       id,
            String       username,
            String       email,
            List<String> roles,
            boolean      enabled,
            boolean      emailVerified,
            Instant      createdAt
    ) {
        public static AdminUserResponse from(User u) {
            return new AdminUserResponse(
                    u.getId().toString(),
                    u.getUsername(),
                    u.getEmail().value(),
                    u.getRoles().stream().map(Enum::name).sorted().toList(),
                    u.isEnabled(),
                    u.isEmailVerified(),
                    u.getCreatedAt()
            );
        }
    }
}
