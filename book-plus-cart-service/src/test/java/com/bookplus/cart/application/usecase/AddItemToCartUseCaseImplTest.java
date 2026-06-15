package com.bookplus.cart.application.usecase;

import com.bookplus.cart.domain.model.*;
import com.bookplus.cart.domain.port.in.AddItemToCartUseCase.AddItemCommand;
import com.bookplus.cart.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddItemToCartUseCaseImpl")
class AddItemToCartUseCaseImplTest {

    @Mock private LoadCartPort             loadCartPort;
    @Mock private SaveCartPort             saveCartPort;
    @Mock private DomainEventPublisherPort eventPublisher;

    @InjectMocks
    private AddItemToCartUseCaseImpl useCase;

    private static final String USER_ID = "user-test";
    private static final Money  PRICE   = Money.of(new BigDecimal("19.99"), "USD");

    /** BookId.of() exige un UUID; derivamos uno determinista del texto. */
    private String uuid(String seed) {
        return java.util.UUID.nameUUIDFromBytes(seed.getBytes()).toString();
    }

    private AddItemCommand cmd(String bookId, int qty) {
        return new AddItemCommand(USER_ID, uuid(bookId), "ISBN-XYZ", "Test Book", null, PRICE, qty);
    }

    /** getItems() devuelve una Collection; localizamos el ítem por bookId. */
    private CartItem item(Cart cart, String bookId) {
        return cart.getItems().stream()
                .filter(i -> i.getBookId().equals(BookId.of(uuid(bookId))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Item no encontrado: " + bookId));
    }

    // ── creates new cart when none exists ─────────────────────────────────

    @Test
    @DisplayName("addItem() creates a new cart when user has none and adds the item")
    void addItem_noExistingCart_createsCartAndAddsItem() {
        given(loadCartPort.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(saveCartPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Cart result = useCase.addItem(cmd("book-1", 2));

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getItems()).hasSize(1);
        assertThat(item(result, "book-1").getQuantity()).isEqualTo(2);

        then(saveCartPort).should().save(any());
    }

    // ── adds to existing cart ─────────────────────────────────────────────

    @Test
    @DisplayName("addItem() adds to an existing cart")
    void addItem_existingCart_addsItem() {
        Cart existing = Cart.createFor(USER_ID);
        existing.pullDomainEvents();

        given(loadCartPort.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        given(saveCartPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        useCase.addItem(cmd("book-1", 1));

        then(saveCartPort).should().save(any());
        assertThat(existing.getItems()).hasSize(1);
    }

    // ── merges quantity for same bookId ───────────────────────────────────

    @Test
    @DisplayName("addItem() merges quantity when same book already in cart")
    void addItem_sameBook_mergesQuantity() {
        Cart existing = Cart.createFor(USER_ID);
        existing.addItem(BookId.of(uuid("book-1")), "Test Book", null, "ISBN-XYZ", 3, PRICE);
        existing.pullDomainEvents();

        given(loadCartPort.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        given(saveCartPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Cart result = useCase.addItem(cmd("book-1", 2));

        assertThat(item(result, "book-1").getQuantity()).isEqualTo(5);
    }

    // ── event publish failure is non-fatal ────────────────────────────────

    @Test
    @DisplayName("addItem() succeeds even when Kafka publish fails")
    void addItem_kafkaFailure_nonFatal() {
        given(loadCartPort.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(saveCartPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("Kafka down")).given(eventPublisher).publishAll(any());

        assertThatNoException().isThrownBy(() -> useCase.addItem(cmd("book-2", 1)));
    }
}
