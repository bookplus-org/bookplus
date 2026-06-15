package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.model.BookId;

/** Puerto — indexar/desindexar libro en Elasticsearch. */
public interface IndexBookPort {
    void index(Book book);
    void remove(BookId bookId);
}
