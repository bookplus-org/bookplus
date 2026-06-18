package com.bookplus.inventory.application.batch;

import com.bookplus.inventory.domain.model.*;
import com.bookplus.inventory.domain.port.out.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationExpiryService (writer del job batch)")
class ReservationExpiryServiceTest {

    @Mock private LoadStockPort            loadStockPort;
    @Mock private SaveStockPort            saveStockPort;
    @Mock private SaveMovementPort         saveMovementPort;
    @Mock private SaveReservationPort      saveReservationPort;
    @Mock private DomainEventPublisherPort eventPublisher;

    @InjectMocks
    private ReservationExpiryService service;

    private StockReservation pending() {
        return StockReservation.create(BookId.of(UUID.randomUUID().toString()), "ORD-1", "user-1", 4);
    }

    @Test
    @DisplayName("expira la reserva, libera el stock, persiste y publica eventos")
    void expire_releasesStock() {
        StockReservation reservation = pending();
        Stock stock = mock(Stock.class);
        given(loadStockPort.findByBookId(any())).willReturn(Optional.of(stock));
        given(stock.releaseReservation(anyInt(), any(), any())).willReturn(mock(StockMovement.class));
        given(stock.pullDomainEvents()).willReturn(List.of());

        service.expire(reservation);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        then(saveStockPort).should().save(stock);
        then(saveMovementPort).should().save(any());
        then(eventPublisher).should().publishAll(anyList());
        then(eventPublisher).should().publish(any());
        then(saveReservationPort).should().save(reservation);
    }

    @Test
    @DisplayName("si no hay stock para el libro, igualmente marca la reserva como expirada")
    void expire_noStock_stillSavesReservation() {
        StockReservation reservation = pending();
        given(loadStockPort.findByBookId(any())).willReturn(Optional.empty());

        service.expire(reservation);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        then(saveStockPort).should(never()).save(any());
        then(saveReservationPort).should().save(reservation);
    }
}
