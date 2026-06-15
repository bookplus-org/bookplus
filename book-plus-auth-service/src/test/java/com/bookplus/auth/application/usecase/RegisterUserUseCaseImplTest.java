package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.UserAlreadyExistsException;
import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.port.in.AuthResult;
import com.bookplus.auth.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.bookplus.auth.domain.port.out.*;
import com.bookplus.auth.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // setUp comparte stubs no usados por todos los tests
@DisplayName("RegisterUserUseCaseImpl — Unit Tests")
class RegisterUserUseCaseImplTest {

    @Mock private LoadUserPort           loadUserPort;
    @Mock private SaveUserPort           saveUserPort;
    @Mock private SaveRefreshTokenPort   saveRefreshTokenPort;
    @Mock private DomainEventPublisherPort eventPublisher;
    @Mock private EmailNotificationPort  emailNotification;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtService             jwtService;

    @InjectMocks
    private RegisterUserUseCaseImpl useCase;

    private static final RegisterUserCommand VALID_COMMAND =
            new RegisterUserCommand("testuser", "test@example.com", "Password123!");

    @BeforeEach
    void setUp() {
        when(loadUserPort.existsByEmail(any(Email.class))).thenReturn(false);
        when(loadUserPort.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token-mock");
        when(jwtService.generateRefreshTokenValue()).thenReturn("refresh-token-mock");
        when(jwtService.buildRefreshToken(any(), any(), any(), any()))
                .thenReturn(mock(com.bookplus.auth.domain.model.RefreshToken.class));
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saveRefreshTokenPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Should register user successfully and return tokens")
    void shouldRegisterSuccessfully() {
        AuthResult result = useCase.register(VALID_COMMAND);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-mock");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-mock");
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.accessTokenExpiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Should save user with hashed password")
    void shouldSaveUserWithHashedPassword() {
        useCase.register(VALID_COMMAND);

        verify(saveUserPort).save(argThat(user ->
                user.getPasswordHash().equals("$2a$12$hashed")
        ));
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    @DisplayName("Should publish domain events after registration")
    void shouldPublishDomainEvents() {
        useCase.register(VALID_COMMAND);

        verify(eventPublisher).publishAll(anyList());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email exists")
    void shouldThrowWhenEmailExists() {
        when(loadUserPort.existsByEmail(Email.of("test@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(VALID_COMMAND))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(saveUserPort, never()).save(any());
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username exists")
    void shouldThrowWhenUsernameExists() {
        when(loadUserPort.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(VALID_COMMAND))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("Should not fail registration if welcome email fails")
    void shouldNotFailIfEmailFails() {
        doThrow(new RuntimeException("SMTP down"))
                .when(emailNotification).sendWelcomeEmail(any(), any());

        assertThatCode(() -> useCase.register(VALID_COMMAND))
                .doesNotThrowAnyException();
    }
}
