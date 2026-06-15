package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

/** Puerto de entrada — búsqueda full-text via Elasticsearch. */
public interface SearchBooksUseCase {

    PagedResult<Book> search(SearchQuery query);

    record SearchQuery(
            String     query,          // texto libre
            int        page,
            int        size
    ) {
        public SearchQuery {
            if (page < 0)   page = 0;
            if (size < 1)   size = 20;
            if (size > 100) size = 100;
        }
    }
}
