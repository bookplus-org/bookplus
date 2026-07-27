package com.bookplus.catalog.domain.model;

import java.time.Instant;

/** Imagen de portada de un libro (vista de dominio; el mapeo BYTEA vive en el adaptador). */
public record BookCover(
        String  bookId,
        byte[]  image,
        String  contentType,
        Instant updatedAt
) {}
