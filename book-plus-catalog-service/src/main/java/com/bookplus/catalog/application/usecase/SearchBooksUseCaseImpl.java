package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase;
import com.bookplus.catalog.domain.port.out.SearchBooksPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import com.bookplus.catalog.shared.featureflags.FeatureFlags;
import org.togglz.core.manager.FeatureManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase @RequiredArgsConstructor @Slf4j
public class SearchBooksUseCaseImpl implements SearchBooksUseCase {

    private final SearchBooksPort searchBooksPort;
    private final FeatureManager  featureManager;

    @Override
    public PagedResult<Book> search(SearchQuery query) {
        // Kill-switch (Togglz): si la búsqueda se desactiva en caliente, devolvemos vacío
        // sin llegar a Elasticsearch (p. ej. para aliviar carga sin desplegar).
        if (!featureManager.isActive(FeatureFlags.BOOK_SEARCH)) {
            log.warn("Feature BOOK_SEARCH desactivada — devolviendo búsqueda vacía");
            return new PagedResult<>(java.util.List.of(), query.page(), query.size(), 0, 0, true, true);
        }
        log.debug("Searching books: q='{}' page={} size={}", query.query(), query.page(), query.size());
        return searchBooksPort.search(query);
    }
}
