package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.ListBooksUseCase;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase @RequiredArgsConstructor
public class ListBooksUseCaseImpl implements ListBooksUseCase {

    private final LoadBookPort loadBookPort;

    @Override
    public PagedResult<Book> list(ListBooksQuery query) {
        return loadBookPort.findAll(query);
    }
}
