package com.bookplus.order.adapter.out.grpc;

import com.bookplus.inventory.grpc.CheckStockRequest;
import com.bookplus.inventory.grpc.CheckStockResponse;
import com.bookplus.inventory.grpc.StockServiceGrpc;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Cliente gRPC del order-service hacia el StockService del inventory-service.
 *
 * Completa el RPC interno de punta a punta: en vez de una llamada REST/JSON,
 * order consulta la disponibilidad de stock por gRPC (HTTP/2 binario, contrato
 * Protobuf compartido en src/main/proto/stock.proto). Contract-first: los stubs
 * {@code StockServiceGrpc} los genera protobuf-maven-plugin en el build.
 *
 * El constructor productivo abre un canal hacia {@code inventory.grpc.host:port};
 * el constructor de paquete acepta un {@link Channel} arbitrario para poder
 * probar el cliente con un canal in-process (sin red).
 */
@Component
public class StockGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(StockGrpcClient.class);

    private final StockServiceGrpc.StockServiceBlockingStub stub;

    public StockGrpcClient(
            @Value("${inventory.grpc.host:localhost}") String host,
            @Value("${inventory.grpc.port:9090}") int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
        log.info("Cliente gRPC de stock apuntando a {}:{}", host, port);
    }

    StockGrpcClient(Channel channel) {
        this.stub = StockServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Consulta la disponibilidad de un libro por gRPC.
     *
     * @return disponibilidad mapeada a un objeto de dominio ligero del order-service.
     */
    public StockAvailability checkStock(String bookId) {
        CheckStockResponse resp = stub
                .withDeadlineAfter(2, TimeUnit.SECONDS)
                .checkStock(CheckStockRequest.newBuilder().setBookId(bookId).build());
        return new StockAvailability(
                resp.getBookId(),
                resp.getAvailable(),
                resp.getQuantityAvailable());
    }

    /** Vista de disponibilidad que expone el cliente al resto del order-service. */
    public record StockAvailability(String bookId, boolean available, int quantityAvailable) {
    }
}
