package com.bookplus.inventory.adapter.in.grpc;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.Stock;
import com.bookplus.inventory.domain.port.in.GetStockUseCase;
import com.bookplus.inventory.grpc.CheckStockRequest;
import com.bookplus.inventory.grpc.CheckStockResponse;
import com.bookplus.inventory.grpc.StockServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba del adaptador gRPC con un canal in-process (sin red ni puerto real):
 * arranca un servidor gRPC en memoria con el {@link StockGrpcService} real y un
 * caso de uso stub, y comprueba que el mapeo dominio → respuesta protobuf es correcto.
 */
class StockGrpcServiceTest {

    private Server server;
    private ManagedChannel channel;
    private StockServiceGrpc.StockServiceBlockingStub stub;

    private final String bookId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws Exception {
        // Caso de uso stub: devuelve un stock con 7 disponibles (total 10).
        GetStockUseCase useCase = id -> {
            Stock stock = Stock.create(BookId.of(id), 10, 2);
            stock.reserve(3, "order-test");   // 10 total, 7 disponibles, 3 reservados
            return stock;
        };

        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new StockGrpcService(useCase))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = StockServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void checkStock_devuelveDisponibilidadDesdeElDominio() {
        CheckStockResponse response = stub.checkStock(
                CheckStockRequest.newBuilder().setBookId(bookId).build());

        assertThat(response.getBookId()).isEqualTo(bookId);
        assertThat(response.getAvailable()).isTrue();
        assertThat(response.getQuantityAvailable()).isEqualTo(7);
        assertThat(response.getQuantityReserved()).isEqualTo(3);
        assertThat(response.getQuantityTotal()).isEqualTo(10);
    }
}
