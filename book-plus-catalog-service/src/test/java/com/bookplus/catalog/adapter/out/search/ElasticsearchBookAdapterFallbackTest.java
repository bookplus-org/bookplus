package com.bookplus.catalog.adapter.out.search;

import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.port.in.PagedResult;
import com.bookplus.catalog.domain.port.in.SearchBooksUseCase.SearchQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ElasticsearchBookAdapter — fallback de Resilience4j")
class ElasticsearchBookAdapterFallbackTest {

    @Mock private BookSearchRepository    repo;
    @Mock private ElasticsearchOperations ops;

    @Test
    @DisplayName("searchFallback devuelve un resultado vacío cuando ES no responde")
    void fallbackReturnsEmpty() {
        ElasticsearchBookAdapter adapter = new ElasticsearchBookAdapter(repo, ops);
        SearchQuery query = new SearchQuery("clean code", 0, 20);

        PagedResult<Book> result = adapter.searchFallback(query, new RuntimeException("ES down"));

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }
}
