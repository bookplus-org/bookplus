package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.UserAlreadyExistsException;
import com.bookplus.auth.domain.exception.UserNotFoundException;
import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.model.UserRole;
import com.bookplus.auth.domain.port.in.AdminUserUseCase;
import com.bookplus.auth.domain.port.out.DomainEventPublisherPort;
import com.bookplus.auth.domain.port.out.LoadUserPort;
import com.bookplus.auth.domain.port.out.SaveUserPort;
import com.bookplus.auth.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AdminUserUseCaseImpl implements AdminUserUseCase {

    private final LoadUserPort             loadUserPort;
    private final SaveUserPort             saveUserPort;
    private final DomainEventPublisherPort eventPublisher;
    private final PasswordEncoder          passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return loadUserPort.findAll();
    }

    @Override
    @Transactional
    public User createUser(String username, String email, String rawPassword, UserRole role) {
        Email emailVo = Email.of(email);
        if (loadUserPort.existsByEmail(emailVo)) {
            throw new UserAlreadyExistsException("email", email);
        }
        if (loadUserPort.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username", username);
        }

        User user = User.create(username, emailVo, passwordEncoder.encode(rawPassword));
        if (role != null && role != UserRole.ROLE_USER) {
            user.assignRole(role);
        }
        User saved = saveUserPort.save(user);
        eventPublisher.publishAll(saved.pullDomainEvents());
        log.info("Admin created user {} with role {}", username, role);
        return saved;
    }

    @Override
    @Transactional
    public User updateProfile(UserId id, String username, String email) {
        User user = loadUserPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        Email emailVo = Email.of(email);
        if (!emailVo.equals(user.getEmail()) && loadUserPort.existsByEmail(emailVo)) {
            throw new UserAlreadyExistsException("email", email);
        }
        if (!username.equals(user.getUsername()) && loadUserPort.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username", username);
        }

        user.changeProfile(username, emailVo);
        User saved = saveUserPort.save(user);
        log.info("Admin updated profile for user {}", id);
        return saved;
    }

    @Override
    @Transactional
    public void resetPassword(UserId id, String newRawPassword) {
        User user = loadUserPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
        user.changePassword(passwordEncoder.encode(newRawPassword));
        saveUserPort.save(user);
        log.info("Admin reset password for user {}", id);
    }

    @Override
    @Transactional
    public User setEnabled(UserId id, boolean enabled, String reason) {
        User user = loadUserPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        if (enabled) {
            user.activate();
        } else if (user.isEnabled()) {
            user.deactivate(reason == null || reason.isBlank() ? "Desactivado por un administrador" : reason);
        }

        User saved = saveUserPort.save(user);
        eventPublisher.publishAll(saved.pullDomainEvents());
        log.info("Admin set user {} enabled={}", id, enabled);
        return saved;
    }

    @Override
    @Transactional
    public User changeRole(UserId id, UserRole role, boolean grant) {
        User user = loadUserPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        if (grant) {
            user.assignRole(role);
        } else {
            user.removeRole(role); // lanza DomainException si se intenta quitar ROLE_USER
        }

        User saved = saveUserPort.save(user);
        log.info("Admin changed role {} (grant={}) for user {}", role, grant, id);
        return saved;
    }
}
