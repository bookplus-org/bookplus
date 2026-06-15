package com.bookplus.auth.domain.model;

import com.bookplus.auth.domain.event.UserRegisteredEvent;
import com.bookplus.auth.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User — Domain Tests")
class UserTest {

    private static final String VALID_USERNAME = "john_doe";
    private static final String VALID_EMAIL    = "john@example.com";
    private static final String PASSWORD_HASH  = "$2a$12$hashed";

    @Nested
    @DisplayName("User.create()")
    class CreateTests {

        @Test
        @DisplayName("Should create user with ROLE_USER by default")
        void shouldCreateUserWithDefaultRole() {
            User user = User.create(VALID_USERNAME, Email.of(VALID_EMAIL), PASSWORD_HASH);

            assertThat(user.getId()).isNotNull();
            assertThat(user.getUsername()).isEqualTo(VALID_USERNAME);
            assertThat(user.getEmail().value()).isEqualTo(VALID_EMAIL);
            assertThat(user.getRoles()).containsExactly(UserRole.ROLE_USER);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Should register UserRegisteredEvent on creation")
        void shouldRegisterDomainEvent() {
            User user = User.create(VALID_USERNAME, Email.of(VALID_EMAIL), PASSWORD_HASH);

            var events = user.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(UserRegisteredEvent.class);
            UserRegisteredEvent event = (UserRegisteredEvent) events.get(0);
            assertThat(event.userId()).isEqualTo(user.getId());
            assertThat(event.email()).isEqualTo(Email.of(VALID_EMAIL));
        }

        @Test
        @DisplayName("Should clear events after pulling")
        void shouldClearEventsAfterPull() {
            User user = User.create(VALID_USERNAME, Email.of(VALID_EMAIL), PASSWORD_HASH);
            user.pullDomainEvents();

            assertThat(user.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("Should reject blank username")
        void shouldRejectBlankUsername() {
            assertThatThrownBy(() -> User.create("", Email.of(VALID_EMAIL), PASSWORD_HASH))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Username must not be blank");
        }

        @Test
        @DisplayName("Should reject username shorter than 3 chars")
        void shouldRejectShortUsername() {
            assertThatThrownBy(() -> User.create("ab", Email.of(VALID_EMAIL), PASSWORD_HASH))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("between 3 and 50");
        }

        @Test
        @DisplayName("Should reject username with special chars")
        void shouldRejectInvalidUsernameChars() {
            assertThatThrownBy(() -> User.create("john doe!", Email.of(VALID_EMAIL), PASSWORD_HASH))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("can only contain");
        }
    }

    @Nested
    @DisplayName("User.deactivate()")
    class DeactivateTests {

        @Test
        @DisplayName("Should deactivate active user")
        void shouldDeactivateUser() {
            User user = User.create(VALID_USERNAME, Email.of(VALID_EMAIL), PASSWORD_HASH);
            user.pullDomainEvents(); // limpiar eventos de creación

            user.deactivate("Policy violation");

            assertThat(user.isEnabled()).isFalse();
            assertThat(user.pullDomainEvents()).hasSize(1);
        }

        @Test
        @DisplayName("Should reject deactivating already inactive user")
        void shouldRejectDoubleDeactivation() {
            User user = User.create(VALID_USERNAME, Email.of(VALID_EMAIL), PASSWORD_HASH);
            user.deactivate("reason");

            assertThatThrownBy(() -> user.deactivate("reason again"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("already deactivated");
        }
    }

    @Nested
    @DisplayName("Email Value Object")
    class EmailTests {

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeToLowercase() {
            Email email = Email.of("John.DOE@EXAMPLE.COM");
            assertThat(email.value()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmail() {
            assertThatThrownBy(() -> Email.of("not-an-email"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Should consider equal emails as equal value objects")
        void shouldSupportValueEquality() {
            Email e1 = Email.of("user@example.com");
            Email e2 = Email.of("USER@EXAMPLE.COM");
            assertThat(e1).isEqualTo(e2);
        }
    }
}
