package com.bookplus.auth.adapter.out.persistence;

import com.bookplus.auth.adapter.out.persistence.entity.RoleEntity;
import com.bookplus.auth.adapter.out.persistence.entity.UserEntity;
import com.bookplus.auth.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.bookplus.auth.adapter.out.persistence.repository.RoleJpaRepository;
import com.bookplus.auth.adapter.out.persistence.repository.UserJpaRepository;
import com.bookplus.auth.domain.model.Email;
import com.bookplus.auth.domain.model.User;
import com.bookplus.auth.domain.model.UserId;
import com.bookplus.auth.domain.port.out.LoadUserPort;
import com.bookplus.auth.domain.port.out.SaveUserPort;
import com.bookplus.auth.shared.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para User.
 * Implementa los puertos LoadUserPort y SaveUserPort usando JPA.
 */
@PersistenceAdapter
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserJpaRepository   userJpaRepository;
    private final RoleJpaRepository   roleJpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userJpaRepository.findByUsernameOrEmail(usernameOrEmail).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public User save(User domain) {
        Set<RoleEntity> roleEntities = domain.getRoles().stream()
                .map(role -> roleJpaRepository.findByName(role)
                        .orElseGet(() -> roleJpaRepository.save(
                                RoleEntity.builder().name(role).build())))
                .collect(Collectors.toSet());

        UserEntity entity = mapper.toEntity(domain, roleEntities);
        UserEntity saved  = userJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
