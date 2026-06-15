package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase.SearchQuery;

/** Puerto para búsqueda full-text — implementado con Elasticsearch. */
public interface SearchBooksPort {
    PagedResult<Book> search(SearchQuery query);
}
