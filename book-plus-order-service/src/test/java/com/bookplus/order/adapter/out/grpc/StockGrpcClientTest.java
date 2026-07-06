package com.bookplus.order.adapter.out.grpc;

import com.bookplus.inventory.grpc.CheckStockRequest;
import com.bookplus.inventory.grpc.CheckStockResponse;
import com.bookplus.inventory.grpc.StockServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración del cliente gRPC de order contra un StockService "falso"
 * levantado en un canal in-process (sin red, sin puertos). Verifica que el cliente
 * serializa la petición y mapea la respuesta Protobuf al objeto de dominio.
 */
class StockGrpcClientTest {

    private Server server;
    private ManagedChannel channel;
    private StockGrpcClient client;

    @BeforeEach
    void setUp() throws Exception {
        // Doble del inventory-service: responde disponibilidad fija.
        StockServiceGrpc.StockServiceImplBase fakeInventory =
                new StockServiceGrpc.StockServiceImplBase() {
                    @Override
                    public void checkStock(CheckStockRequest request,
                                           StreamObserver<CheckStockResponse> obs) {
                        obs.onNext(CheckStockResponse.newBuilder()
                                .setBookId(request.getBookId())
                                .setAvailable(true)
                                .setQuantityAvailable(42)
                                .setQuantityReserved(3)
                                .setQuantityTotal(45)
                                .build());
                        obs.onCompleted();
                    }
                };

        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(fakeInventory).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new StockGrpcClient(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void checkStock_llamaAInventoryYMapeaLaRespuesta() {
        StockGrpcClient.StockAvailability av = client.checkStock("book-123");

        assertThat(av.bookId()).isEqualTo("book-123");
        assertThat(av.available()).isTrue();
        assertThat(av.quantityAvailable()).isEqualTo(42);
    }
}
