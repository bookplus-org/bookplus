package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.StockMovement;
import com.bookplus.inventory.domain.port.in.GetStockMovementsUseCase;
import com.bookplus.inventory.domain.port.out.LoadMovementPort;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@UseCase @RequiredArgsConstructor
public class GetStockMovementsUseCaseImpl implements GetStockMovementsUseCase {

    private final LoadMovementPort loadMovementPort;

    @Override
    public List<StockMovement> getByBookId(String bookId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return loadMovementPort.findByBookId(BookId.of(bookId), pageable).getContent();
    }
}
