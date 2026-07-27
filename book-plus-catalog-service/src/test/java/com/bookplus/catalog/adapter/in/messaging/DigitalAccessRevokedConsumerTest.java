package com.bookplus.catalog.adapter.in.messaging;

import com.bookplus.catalog.domain.port.in.PurchaseAccessUseCase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DigitalAccessRevokedConsumer")
class DigitalAccessRevokedConsumerTest {

    @Mock private PurchaseAccessUseCase purchaseAccessUseCase;

    @InjectMocks private DigitalAccessRevokedConsumer consumer;

    private final UUID bookId = UUID.randomUUID();
    private static final String USER = "user-1";

    private Map<String, Object> payload(String userId, UUID... books) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (UUID b : books) items.add(Map.of("bookId", b.toString()));
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("items", items);
        return p;
    }

    @Test
    @DisplayName("delega la revocación en el caso de uso, por cada libro")
    void delegatesRevoke() {
        consumer.onAccessRevoked(payload(USER, bookId));
        then(purchaseAccessUseCase).should().revokeAccess(USER, bookId.toString());
    }

    @Test
    @DisplayName("ignora el evento sin userId")
    void ignoresMissingUser() {
        consumer.onAccessRevoked(payload(null, bookId));
        then(purchaseAccessUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("no propaga si el caso de uso lanza (best-effort por ítem)")
    void swallowsUseCaseError() {
        willThrow(new RuntimeException("boom"))
                .given(purchaseAccessUseCase).revokeAccess(USER, bookId.toString());
        assertThatNoException().isThrownBy(() -> consumer.onAccessRevoked(payload(USER, bookId)));
    }
}
