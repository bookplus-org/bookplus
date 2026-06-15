package com.bookplus.auth.domain.model;

/**
 * Value Object enum — Roles del sistema.
 * Los roles son jerárquicos: SUPERADMIN > ADMIN > EDITOR > USER
 */
public enum UserRole {
    ROLE_USER,
    ROLE_REPARTIDOR,   // rol funcional: gestiona entregas físicas
    ROLE_EDITOR,
    ROLE_ADMIN,
    ROLE_SUPERADMIN;

    public String authority() {
        return this.name();
    }

    public boolean isAtLeast(UserRole required) {
        return this.ordinal() >= required.ordinal();
    }
}
