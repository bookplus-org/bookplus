package com.bookplus.inventory.adapter.in.grpc;

import com.bookplus.inventory.domain.model.Stock;
import com.bookplus.inventory.domain.port.in.GetStockUseCase;
import com.bookplus.inventory.grpc.CheckStockRequest;
import com.bookplus.inventory.grpc.CheckStockResponse;
import com.bookplus.inventory.grpc.StockServiceGrpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Adaptador de entrada gRPC — expone la consulta de stock por HTTP/2 binario.
 *
 * Arquitectura hexagonal: reutiliza el MISMO caso de uso que el adaptador REST
 * ({@link GetStockUseCase}); el dominio no sabe si lo invocan por REST o por gRPC.
 * Los stubs {@code StockServiceGrpc}, {@code CheckStockRequest/Response} los genera
 * protobuf-maven-plugin desde {@code src/main/proto/stock.proto} en tiempo de build.
 *
 * {@link GrpcService} (net.devh) registra este bean en el servidor gRPC embebido
 * (puerto 9090 por defecto) que arranca junto al contexto de Spring.
 */
@GrpcService
public class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {

    private final GetStockUseCase getStockUseCase;

    public StockGrpcService(GetStockUseCase getStockUseCase) {
        this.getStockUseCase = getStockUseCase;
    }

    @Override
    public void checkStock(CheckStockRequest request,
                           StreamObserver<CheckStockResponse> responseObserver) {
        Stock stock = getStockUseCase.getByBookId(request.getBookId());

        CheckStockResponse response = CheckStockResponse.newBuilder()
                .setBookId(request.getBookId())
                .setAvailable(stock.getQuantityAvailable() > 0)
                .setQuantityAvailable(stock.getQuantityAvailable())
                .setQuantityReserved(stock.getQuantityReserved())
                .setQuantityTotal(stock.getQuantityTotal())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
