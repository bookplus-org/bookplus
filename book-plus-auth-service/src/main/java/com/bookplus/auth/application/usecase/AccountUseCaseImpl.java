package com.bookplus.auth.application.usecase;

import com.bookplus.auth.domain.exception.DomainException;
import com.bookplus.auth.domain.exception.UserAlreadyExistsException;
import com.bookplus.auth.domain.exception.UserNotFoundException;
import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.in.AccountUseCase;
import com.bookplus.auth.domain.port.out.LoadUserPort;
import com.bookplus.auth.domain.port.out.SaveUserPort;
import com.bookplus.auth.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AccountUseCaseImpl implements AccountUseCase {

    private final LoadUserPort    loadUserPort;
    private final SaveUserPort    saveUserPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User updateMyProfile(UserId id, String username, String email) {
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
        return saveUserPort.save(user);
    }

    @Override
    @Transactional
    public void changeMyPassword(UserId id, String currentPassword, String newPassword) {
        User user = loadUserPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new DomainException("La contraseña actual no es correcta");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        saveUserPort.save(user);
        log.info("User {} changed own password", id);
    }
}
