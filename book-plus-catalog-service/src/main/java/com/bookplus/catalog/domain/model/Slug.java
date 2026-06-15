package com.bookplus.catalog.domain.model;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Value Object — Slug URL-friendly generado a partir de un texto.
 * Ej: "El Señor de los Anillos" → "el-senor-de-los-anillos"
 */
public record Slug(String value) {

    public Slug {
        Objects.requireNonNull(value, "Slug must not be null");
        if (value.isBlank()) throw new IllegalArgumentException("Slug must not be blank");
    }

    public static Slug of(String value) { return new Slug(value); }

    public static Slug from(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s_-]+", "-")
                .replaceAll("^-|-$", "");
        return new Slug(normalized);
    }

    @Override public String toString() { return value; }
}
