package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.model.BookCover;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.port.in.BookCoverUseCase;
import com.bookplus.catalog.domain.port.out.BookCoverPort;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.domain.port.out.SaveBookPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class BookCoverUseCaseImpl implements BookCoverUseCase {

    private final BookCoverPort bookCoverPort;
    private final LoadBookPort  loadBookPort;
    private final SaveBookPort  saveBookPort;

    @Override
    public Optional<BookCover> getCover(String bookId) {
        return bookCoverPort.findByBookId(bookId);
    }

    @Override
    public String uploadCover(String bookId, byte[] image, String contentType) {
        Book book = loadBookPort.findById(BookId.of(bookId))
                .orElseThrow(() -> new BookNotFoundException(bookId));

        bookCoverPort.save(new BookCover(bookId, image, contentType, Instant.now()));

        String imageUrl = "/api/v1/books/" + bookId + "/cover";
        book.changeCoverImageUrl(imageUrl);
        saveBookPort.save(book);
        return imageUrl;
    }
}
