package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.port.in.ManageFavoritesUseCase;
import com.bookplus.catalog.domain.port.out.FavoritePort;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

@UseCase
@RequiredArgsConstructor
public class ManageFavoritesUseCaseImpl implements ManageFavoritesUseCase {

    private final FavoritePort favoritePort;
    private final LoadBookPort loadBookPort;

    @Override
    public List<Book> listFavoriteBooks(String userId) {
        return favoritePort.findBookIdsByUser(userId).stream()
                .map(id -> loadBookPort.findById(BookId.of(id)).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<String> listFavoriteIds(String userId) {
        return favoritePort.findBookIdsByUser(userId);
    }

    @Override
    public void add(String userId, String bookId) {
        if (!favoritePort.exists(userId, bookId)) {
            favoritePort.add(userId, bookId);
        }
    }

    @Override
    public void remove(String userId, String bookId) {
        favoritePort.remove(userId, bookId);
    }
}
