package com.bookplus.catalog.integration;

import com.bookplus.catalog.adapter.out.persistence.entity.BookEntity;
import com.bookplus.catalog.adapter.out.persistence.entity.UserPurchaseEntity;
import com.bookplus.catalog.adapter.out.persistence.repository.BookJpaRepository;
import com.bookplus.catalog.adapter.out.persistence.repository.UserPurchaseJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de INTEGRACIÓN (Testcontainers): levanta un PostgreSQL real, aplica todas las
 * migraciones Flyway y verifica el esquema y las consultas de la biblioteca contra una
 * base de datos auténtica — algo que los tests unitarios con mocks no cubren.
 *
 * Etiquetado como "integration": se excluye del `mvn test` normal y se ejecuta con
 * `mvn test -Dgroups=integration` en un entorno con Docker disponible.
 */
@Tag("integration")
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("CatalogSchemaIT — Flyway + Postgres real")
class CatalogSchemaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private BookJpaRepository         books;
    @Autowired private UserPurchaseJpaRepository purchases;

    @Test
    @DisplayName("las migraciones Flyway aplican y cargan datos semilla")
    void migrationsApplyAndSeedLoads() {
        assertThat(books.count()).isPositive();
    }

    @Test
    @DisplayName("las columnas de tracking de la biblioteca (V10) funcionan")
    void purchaseTrackingColumnsWork() {
        BookEntity anyBook = books.findAll().get(0);
        purchases.save(UserPurchaseEntity.builder()
                .userId("it-user").bookId(anyBook.getId()).purchasedAt(Instant.now())
                .active(true).downloaded(true).readProgress(42).build());

        assertThat(purchases.existsByUserIdAndBookIdAndActiveTrue("it-user", anyBook.getId())).isTrue();
        UserPurchaseEntity found = purchases.findByUserIdAndBookId("it-user", anyBook.getId()).orElseThrow();
        assertThat(found.getReadProgress()).isEqualTo(42);
        assertThat(found.isDownloaded()).isTrue();
    }
}
