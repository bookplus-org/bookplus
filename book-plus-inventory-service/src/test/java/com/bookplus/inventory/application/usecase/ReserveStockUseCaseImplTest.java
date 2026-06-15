package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.InsufficientStockException;
import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.in.ReserveStockUseCase.ReserveStockCommand;
import com.bookplus.inventory.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveStockUseCaseImpl")
class ReserveStockUseCaseImplTest {

    @Mock private LoadStockPort            loadStockPort;
    @Mock private SaveStockPort            saveStockPort;
    @Mock private SaveReservationPort      saveReservationPort;
    @Mock private SaveMovementPort         saveMovementPort;
    @Mock private DomainEventPublisherPort eventPublisher;

    @InjectMocks
    private ReserveStockUseCaseImpl useCase;

    private BookId bookId;
    private Stock  stock;

    @BeforeEach
    void setUp() {
        bookId = BookId.generate();
        stock  = Stock.create(bookId, 100, 5);
        stock.pullDomainEvents(); // limpiar evento de creación
    }

    @Test
    @DisplayName("reserve() — crea reserva y actualiza stock correctamente")
    void reserve_success() {
        given(loadStockPort.findByBookId(bookId)).willReturn(Optional.of(stock));
        given(saveStockPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveReservationPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveMovementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        ReserveStockCommand command =
                new ReserveStockCommand(bookId.toString(), "ORD-001", "user-1", 10);

        StockReservation result = useCase.reserve(command);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getOrderId()).isEqualTo("ORD-001");
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(stock.getQuantityAvailable()).isEqualTo(90);
        assertThat(stock.getQuantityReserved()).isEqualTo(10);

        then(saveStockPort).should().save(any());
        then(saveReservationPort).should().save(any());
        then(saveMovementPort).should().save(any());
    }

    @Test
    @DisplayName("reserve() lanza StockNotFoundException si el libro no tiene stock registrado")
    void reserve_stockNotFound_shouldThrow() {
        given(loadStockPort.findByBookId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reserve(
                new ReserveStockCommand(bookId.toString(), "ORD-001", "user-1", 5)))
                .isInstanceOf(StockNotFoundException.class);

        then(saveStockPort).should(never()).save(any());
    }

    @Test
    @DisplayName("reserve() lanza InsufficientStockException si no hay stock suficiente")
    void reserve_insufficient_shouldThrow() {
        given(loadStockPort.findByBookId(bookId)).willReturn(Optional.of(stock));

        assertThatThrownBy(() -> useCase.reserve(
                new ReserveStockCommand(bookId.toString(), "ORD-001", "user-1", 200)))
                .isInstanceOf(InsufficientStockException.class);

        then(saveStockPort).should(never()).save(any());
        then(saveReservationPort).should(never()).save(any());
    }
}
