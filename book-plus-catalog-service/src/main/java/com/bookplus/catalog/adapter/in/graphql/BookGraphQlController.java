package com.bookplus.catalog.adapter.in.graphql;

import com.bookplus.catalog.adapter.in.web.dto.BookResponse;
import com.bookplus.catalog.domain.port.in.GetBookUseCase;
import com.bookplus.catalog.domain.port.in.ListBooksUseCase;
import com.bookplus.catalog.domain.port.in.ListBooksUseCase.ListBooksQuery;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Adaptador de entrada GraphQL — expone el catálogo como API de agregación.
 *
 * Reutiliza los MISMOS casos de uso que el adaptador REST (arquitectura hexagonal:
 * el dominio no sabe si lo invocan por REST o por GraphQL) y el mapper existente
 * {@link BookResponse#from}. El frontend puede pedir en una sola consulta los libros
 * con exactamente los campos que necesita cada vista.
 */
@Controller
public class BookGraphQlController {

    private final GetBookUseCase   getBookUseCase;
    private final ListBooksUseCase listBooksUseCase;

    public BookGraphQlController(GetBookUseCase getBookUseCase, ListBooksUseCase listBooksUseCase) {
        this.getBookUseCase   = getBookUseCase;
        this.listBooksUseCase = listBooksUseCase;
    }

    @QueryMapping
    public BookResponse book(@Argument String id) {
        return BookResponse.from(getBookUseCase.getById(id));
    }

    @QueryMapping
    public List<BookResponse> books(@Argument int page, @Argument int size,
                                    @Argument String categoryId, @Argument String author) {
        return listBooksUseCase.list(new ListBooksQuery(page, size, categoryId, author))
                .content().stream()
                .map(BookResponse::from)
                .toList();
    }
}
