package com.bookplus.inventory.application.usecase;

import com.bookplus.inventory.domain.exception.StockNotFoundException;
import com.bookplus.inventory.domain.model.BookId;
import com.bookplus.inventory.domain.model.Stock;
import com.bookplus.inventory.domain.port.in.GetStockUseCase;
import com.bookplus.inventory.domain.port.out.LoadStockPort;
import com.bookplus.inventory.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class GetStockUseCaseImpl implements GetStockUseCase {

    private final LoadStockPort loadStockPort;

    @Override
    public Stock getByBookId(String bookId) {
        return loadStockPort.findByBookId(BookId.of(bookId))
                .orElseThrow(() -> new StockNotFoundException(bookId));
    }
}
