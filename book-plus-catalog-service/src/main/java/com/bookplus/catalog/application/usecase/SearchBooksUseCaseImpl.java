package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase;
import com.bookplus.catalog.domain.port.out.SearchBooksPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class SearchBooksUseCaseImpl implements SearchBooksUseCase {

    private final SearchBooksPort searchBooksPort;

    @Override
    public PagedResult<Book> search(SearchQuery query) {
        log.debug("Searching books: q='{}' page={} size={}", query.query(), query.page(), query.size());
        return searchBooksPort.search(query);
    }
}
