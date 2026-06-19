package com.bookplus.catalog.shared.featureflags;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;

/**
 * Feature flags de catalog-service (Togglz).
 *
 * Permiten activar/desactivar funciones en caliente, sin desplegar, desde la consola de
 * administración (/togglz-console). Útil para canary releases y, sobre todo, como
 * kill-switch operativo: apagar una función pesada bajo carga sin tocar el código.
 */
public enum FeatureFlags implements Feature {

    @Label("Búsqueda de libros (kill-switch). Si se desactiva, /search devuelve vacío sin tocar Elasticsearch.")
    @EnabledByDefault
    BOOK_SEARCH;
}
