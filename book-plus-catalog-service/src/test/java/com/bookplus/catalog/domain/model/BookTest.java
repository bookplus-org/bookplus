package com.bookplus.catalog.domain.model;

import com.bookplus.catalog.domain.event.BookCreatedEvent;
import com.bookplus.catalog.domain.event.BookDeletedEvent;
import com.bookplus.catalog.domain.event.BookUpdatedEvent;
import com.bookplus.catalog.domain.event.DomainEvent;
import com.bookplus.catalog.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Book Aggregate")
class BookTest {

    private ISBN       isbn;
    private Money      price;
    private CategoryId categoryId;

    @BeforeEach
    void setUp() {
        isbn       = ISBN.of("9780132350884");
        price      = Money.of(new BigDecimal("39.99"), "USD");
        categoryId = CategoryId.generate();
    }

    // ── Factory ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() genera un Book con estado correcto y emite BookCreatedEvent")
    void create_shouldSetFieldsAndPublishEvent() {
        Book book = Book.create(isbn, "Clean Code", "Robert C. Martin",
                "Great book", price, null, null, "Publisher", LocalDate.of(2008, 8, 1),
                "en", 431, categoryId);

        assertThat(book.getId()).isNotNull();
        assertThat(book.getIsbn()).isEqualTo(isbn);
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.isActive()).isTrue();
        assertThat(book.getStockSnapshot()).isZero();
        assertThat(book.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(book.getReviewCount()).isZero();

        List<DomainEvent> events = book.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(BookCreatedEvent.class);
    }

    @Test
    @DisplayName("pullDomainEvents() vacía la lista interna")
    void pullDomainEvents_shouldClearEvents() {
        Book book = buildBook();
        book.pullDomainEvents(); // primera llamada
        assertThat(book.pullDomainEvents()).isEmpty();
    }

    // ── Validación de dominio ─────────────────────────────────────────────

    @Test
    @DisplayName("create() lanza DomainException si el título está en blanco")
    void create_shouldThrowIfTitleBlank() {
        assertThatThrownBy(() ->
                Book.create(isbn, "  ", "Author", null, price, null, null, null, null, null, null, categoryId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("create() lanza DomainException si el autor está en blanco")
    void create_shouldThrowIfAuthorBlank() {
        assertThatThrownBy(() ->
                Book.create(isbn, "Title", "", null, price, null, null, null, null, null, null, categoryId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Author");
    }

    // ── update() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() cambia campos y emite BookUpdatedEvent")
    void update_shouldUpdateFieldsAndPublishEvent() {
        Book book = buildBook();
        book.pullDomainEvents(); // limpiar evento de creación

        Money newPrice = Money.of(new BigDecimal("44.99"), "USD");
        book.update("Clean Code 2nd Ed.", "Robert C. Martin", "Updated description",
                newPrice, null, null, null, null, null, null, null, categoryId);

        assertThat(book.getTitle()).isEqualTo("Clean Code 2nd Ed.");
        assertThat(book.getPrice()).isEqualTo(newPrice);

        List<DomainEvent> events = book.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(BookUpdatedEvent.class);
    }

    // ── deactivate() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate() marca el libro como inactivo y emite BookDeletedEvent")
    void deactivate_shouldSetInactiveAndPublishEvent() {
        Book book = buildBook();
        book.pullDomainEvents();

        book.deactivate();

        assertThat(book.isActive()).isFalse();
        List<DomainEvent> events = book.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(BookDeletedEvent.class);
    }

    @Test
    @DisplayName("deactivate() sobre libro ya inactivo lanza DomainException")
    void deactivate_twiceShouldThrow() {
        Book book = buildBook();
        book.deactivate();
        assertThatThrownBy(book::deactivate).isInstanceOf(DomainException.class);
    }

    // ── applyDiscount() ───────────────────────────────────────────────────

    @Test
    @DisplayName("applyDiscount() establece el precio de descuento correctamente")
    void applyDiscount_shouldSetDiscountPrice() {
        Book book = buildBook();
        Money discount = Money.of(new BigDecimal("29.99"), "USD");

        book.applyDiscount(discount);

        assertThat(book.hasDiscount()).isTrue();
        assertThat(book.getDiscountPrice()).isEqualTo(discount);
        assertThat(book.effectivePrice()).isEqualTo(discount);
    }

    @Test
    @DisplayName("applyDiscount() lanza DomainException si el descuento es mayor al precio")
    void applyDiscount_higherThanPriceShouldThrow() {
        Book book = buildBook();
        Money tooHigh = Money.of(new BigDecimal("99.99"), "USD");

        assertThatThrownBy(() -> book.applyDiscount(tooHigh))
                .isInstanceOf(DomainException.class);
    }

    // ── addReviewStats() ─────────────────────────────────────────────────

    @Test
    @DisplayName("addReviewStats() recalcula el rating promedio correctamente")
    void addReviewStats_shouldRecalculateAverage() {
        Book book = buildBook();

        book.addReviewStats(Rating.of(4));
        assertThat(book.getReviewCount()).isEqualTo(1);
        assertThat(book.getAverageRating()).isEqualByComparingTo("4.00");

        book.addReviewStats(Rating.of(2));
        assertThat(book.getReviewCount()).isEqualTo(2);
        assertThat(book.getAverageRating()).isEqualByComparingTo("3.00");
    }

    // ── updateStock() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStock() actualiza el snapshot y no permite negativos")
    void updateStock_shouldClampToZero() {
        Book book = buildBook();
        book.updateStock(100);
        assertThat(book.getStockSnapshot()).isEqualTo(100);
        assertThat(book.isInStock()).isTrue();

        book.updateStock(-5);
        assertThat(book.getStockSnapshot()).isZero();
        assertThat(book.isInStock()).isFalse();
    }

    // ── ISBN ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ISBN inválido lanza DomainException")
    void isbn_invalidShouldThrow() {
        assertThatThrownBy(() -> ISBN.of("1234567890123"))
                .isInstanceOf(DomainException.class);
    }

    // ── Money ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Money negativo lanza DomainException")
    void money_negativeShouldThrow() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1.00"), "USD"))
                .isInstanceOf(DomainException.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Book buildBook() {
        return Book.create(isbn, "Clean Code", "Robert C. Martin",
                "Great book about clean code.", price, null, null,
                "Prentice Hall", LocalDate.of(2008, 8, 1), "en", 431, categoryId);
    }
}
