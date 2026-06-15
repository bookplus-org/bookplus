package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.event.BookCreatedEvent;
import com.bookplus.catalog.domain.event.DomainEvent;
import com.bookplus.catalog.domain.exception.BookAlreadyExistsException;
import com.bookplus.catalog.domain.exception.CategoryNotFoundException;
import com.bookplus.catalog.domain.model.*;
import com.bookplus.catalog.domain.port.in.CreateBookUseCase.CreateBookCommand;
import com.bookplus.catalog.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBookUseCaseImpl")
class CreateBookUseCaseImplTest {

    @Mock private LoadBookPort             loadBookPort;
    @Mock private SaveBookPort             saveBookPort;
    @Mock private LoadCategoryPort         loadCategoryPort;
    @Mock private IndexBookPort            indexBookPort;
    @Mock private DomainEventPublisherPort eventPublisher;

    @InjectMocks
    private CreateBookUseCaseImpl useCase;

    private CreateBookCommand command;
    private Category          category;

    @BeforeEach
    void setUp() {
        command = new CreateBookCommand(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                "A handbook of agile software craftsmanship",
                new BigDecimal("39.99"),
                "USD",
                null,                                   // discountPrice
                "https://example.com/clean-code.jpg",
                null,                                   // previewUrl
                "Prentice Hall",
                LocalDate.of(2008, 8, 1),
                "en",
                431,
                "11111111-0000-0000-0000-000000000009"
        );

        category = Category.create("Software Engineering", "SE books", null, null);
    }

    // ── Happy path ────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() — flujo exitoso persiste libro, indexa en ES y publica evento")
    void create_success() {
        given(loadBookPort.existsByIsbn(any())).willReturn(false);
        given(loadCategoryPort.findById(any())).willReturn(Optional.of(category));
        given(saveBookPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(indexBookPort).index(any());
        willDoNothing().given(eventPublisher).publishAll(anyList());

        Book result = useCase.create(command);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getIsbn().value()).isEqualTo("9780132350884");
        assertThat(result.isActive()).isTrue();

        then(saveBookPort).should().save(any(Book.class));
        then(indexBookPort).should().index(any(Book.class));
        then(eventPublisher).should().publishAll(argThat(events ->
                events.stream().anyMatch(e -> e instanceof BookCreatedEvent)));
    }

    // ── ISBN duplicado ────────────────────────────────────────────────────

    @Test
    @DisplayName("create() lanza BookAlreadyExistsException si el ISBN ya existe")
    void create_duplicateIsbn_throws() {
        given(loadBookPort.existsByIsbn(any())).willReturn(true);

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(BookAlreadyExistsException.class);

        then(saveBookPort).should(never()).save(any());
        then(eventPublisher).should(never()).publishAll(any());
    }

    // ── Categoría inexistente ─────────────────────────────────────────────

    @Test
    @DisplayName("create() lanza CategoryNotFoundException si la categoría no existe")
    void create_categoryNotFound_throws() {
        given(loadBookPort.existsByIsbn(any())).willReturn(false);
        given(loadCategoryPort.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(CategoryNotFoundException.class);

        then(saveBookPort).should(never()).save(any());
    }

    // ── Fallo de indexación no fatal ──────────────────────────────────────

    @Test
    @DisplayName("create() continúa aunque Elasticsearch falle (degradación graceful)")
    void create_elasticsearchFailure_nonFatal() {
        given(loadBookPort.existsByIsbn(any())).willReturn(false);
        given(loadCategoryPort.findById(any())).willReturn(Optional.of(category));
        given(saveBookPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("ES down")).given(indexBookPort).index(any());
        willDoNothing().given(eventPublisher).publishAll(anyList());

        // No debe lanzar excepción
        assertThatCode(() -> useCase.create(command)).doesNotThrowAnyException();

        // El evento Kafka aún debe publicarse
        then(eventPublisher).should().publishAll(anyList());
    }

    // ── Categoría inactiva ────────────────────────────────────────────────

    @Test
    @DisplayName("create() lanza DomainException si la categoría está inactiva")
    void create_inactiveCategory_throws() {
        category.deactivate();
        given(loadBookPort.existsByIsbn(any())).willReturn(false);
        given(loadCategoryPort.findById(any())).willReturn(Optional.of(category));

        // Una categoría inactiva se trata como no disponible para asignar libros.
        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(com.bookplus.catalog.domain.exception.DomainException.class)
                .hasMessageContaining("not found");

        then(saveBookPort).should(never()).save(any());
    }
}
