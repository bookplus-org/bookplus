package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Book;

public interface SaveBookPort {
    Book save(Book book);
}
