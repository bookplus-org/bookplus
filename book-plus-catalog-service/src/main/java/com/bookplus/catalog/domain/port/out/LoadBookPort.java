package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.ISBN;
import com.bookplus.catalog.domain.model.Slug;
import com.bookplus.catalog.domain.port.in.ListBooksUseCase.ListBooksQuery;
import com.bookplus.catalog.domain.port.in.PagedResult;

import java.util.Optional;

public interface LoadBookPort {
    Optional<Book> findById(BookId id);
    Optional<Book> findByIsbn(ISBN isbn);
    Optional<Book> findBySlug(Slug slug);
    boolean existsByIsbn(ISBN isbn);
    PagedResult<Book> findAll(ListBooksQuery query);
}
