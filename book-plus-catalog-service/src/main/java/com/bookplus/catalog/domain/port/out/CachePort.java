package com.bookplus.catalog.domain.port.out;

import com.bookplus.catalog.domain.model.Book;

import java.util.Optional;

/** Puerto — caché de libros en Redis para lecturas frecuentes. */
public interface CachePort {
    Optional<Book> getBook(String key);
    void putBook(String key, Book book);
    void evictBook(String key);
    void evictByPattern(String pattern);
}
