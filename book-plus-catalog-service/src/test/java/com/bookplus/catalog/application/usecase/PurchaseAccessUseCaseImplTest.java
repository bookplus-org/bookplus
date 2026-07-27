package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Purchase;
import com.bookplus.catalog.domain.port.out.PurchasePort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseAccessUseCaseImpl")
class PurchaseAccessUseCaseImplTest {

    @Mock private PurchasePort purchasePort;

    @InjectMocks private PurchaseAccessUseCaseImpl useCase;

    private static final String USER = "user-1";
    private static final String BOOK = "11111111-1111-1111-1111-111111111111";

    private Purchase purchase(boolean active) {
        return new Purchase(USER, BOOK, Instant.now(), active, false, 0);
    }

    @Test
    @DisplayName("grant: crea la compra si no existe")
    void grant_createsWhenMissing() {
        given(purchasePort.find(USER, BOOK)).willReturn(Optional.empty());
        useCase.grantAccess(USER, BOOK);
        then(purchasePort).should().save(any(Purchase.class));
    }

    @Test
    @DisplayName("grant: idempotente si ya existe")
    void grant_idempotent() {
        given(purchasePort.find(USER, BOOK)).willReturn(Optional.of(purchase(true)));
        useCase.grantAccess(USER, BOOK);
        then(purchasePort).should(never()).save(any());
    }

    @Test
    @DisplayName("revoke: desactiva la compra activa")
    void revoke_deactivates() {
        given(purchasePort.find(USER, BOOK)).willReturn(Optional.of(purchase(true)));
        useCase.revokeAccess(USER, BOOK);
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        then(purchasePort).should().save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().active()).isFalse();
    }

    @Test
    @DisplayName("revoke: idempotente si ya está inactiva")
    void revoke_idempotent() {
        given(purchasePort.find(USER, BOOK)).willReturn(Optional.of(purchase(false)));
        useCase.revokeAccess(USER, BOOK);
        then(purchasePort).should(never()).save(any());
    }
}
