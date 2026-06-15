package com.bookplus.catalog.domain.port.in;

import com.bookplus.catalog.domain.model.Book;

/** Puerto de entrada — listar libros con filtros y paginación. */
public interface ListBooksUseCase {

    PagedResult<Book> list(ListBooksQuery query);

    record ListBooksQuery(
            int    page,
            int    size,
            String categoryId,
            String author
    ) {
        public ListBooksQuery {
            if (page < 0)  page = 0;
            if (size < 1)  size = 20;
            if (size > 100) size = 100;
        }
    }
}
