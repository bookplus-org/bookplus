package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase.SearchQuery;
import com.bookplus.catalog.domain.port.out.SearchBooksPort;
import com.bookplus.catalog.shared.featureflags.FeatureFlags;
import org.junit.jupiter.api.Test;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica el kill-switch de búsqueda con Togglz, sin Spring ni Elasticsearch.
 * Construye un FeatureManager en memoria y comprueba el comportamiento con la flag on/off.
 */
class SearchBooksFeatureFlagTest {

    private final SearchBooksPort port = mock(SearchBooksPort.class);

    private FeatureManager managerWith(boolean bookSearchActive) {
        FeatureManager fm = new FeatureManagerBuilder()
                .featureEnum(FeatureFlags.class)
                .stateRepository(new InMemoryStateRepository())
                .userProvider(new NoOpUserProvider())
                .build();
        fm.setFeatureState(new FeatureState(FeatureFlags.BOOK_SEARCH, bookSearchActive));
        return fm;
    }

    @Test
    void con_la_flag_activa_delega_en_el_puerto_de_busqueda() {
        var paged = new PagedResult<Book>(List.of(), 0, 20, 0, 0, true, true);
        when(port.search(any())).thenReturn(paged);
        var useCase = new SearchBooksUseCaseImpl(port, managerWith(true));

        var result = useCase.search(new SearchQuery("java", 0, 20));

        assertThat(result).isSameAs(paged);
        verify(port).search(any());
    }

    @Test
    void con_la_flag_desactivada_devuelve_vacio_sin_tocar_el_puerto() {
        var useCase = new SearchBooksUseCaseImpl(port, managerWith(false));

        var result = useCase.search(new SearchQuery("java", 0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verifyNoInteractions(port);   // no se llega a Elasticsearch
    }
}
