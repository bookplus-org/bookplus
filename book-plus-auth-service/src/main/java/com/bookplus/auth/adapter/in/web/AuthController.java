package com.bookplus.auth.adapter.in.web;

import com.bookplus.auth.adapter.in.web.dto.request.*;
import com.bookplus.auth.adapter.in.web.dto.response.AuthResponse;
import com.bookplus.auth.domain.port.in.*;
import com.bookplus.auth.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Adaptador web — Controller REST de autenticación.
 * Delega todo al use case correspondiente. CERO lógica de negocio aquí.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Refresh, Logout, Password Reset")
public class AuthController {

    private final RegisterUserUseCase   registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase   refreshTokenUseCase;
    private final LogoutUseCase         logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase  resetPasswordUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        var result = registerUserUseCase.register(
                new RegisterUserUseCase.RegisterUserCommand(
                        request.username(),
                        request.email(),
                        request.password()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", AuthResponse.from(result)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        var result = authenticateUserUseCase.authenticate(
                new AuthenticateUserUseCase.AuthenticateCommand(
                        request.usernameOrEmail(),
                        request.password(),
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("User-Agent")));

        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.from(result)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        var result = refreshTokenUseCase.refresh(
                new RefreshTokenUseCase.RefreshCommand(
                        request.refreshToken(),
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("User-Agent")));

        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.from(result)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token and blacklist access token")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String accessToken = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : authHeader;

        logoutUseCase.logout(new LogoutUseCase.LogoutCommand(request.refreshToken(), accessToken));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email (always returns 200)")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        forgotPasswordUseCase.requestReset(
                new ForgotPasswordUseCase.ForgotPasswordCommand(request.email()));

        return ResponseEntity.ok(ApiResponse.ok(
                "If that email is registered, you will receive a reset link shortly"));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset password using the token from email")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        resetPasswordUseCase.reset(
                new ResetPasswordUseCase.ResetPasswordCommand(
                        request.token(), request.newPassword()));

        return ResponseEntity.noContent().build();
    }
}
