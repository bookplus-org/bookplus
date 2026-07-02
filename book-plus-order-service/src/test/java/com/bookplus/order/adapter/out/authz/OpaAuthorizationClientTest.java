package com.bookplus.order.adapter.out.authz;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del cliente OPA sin red: verifican la construcción del input que
 * espera la política y el comportamiento en modo shadow (deshabilitado).
 * La evaluación de las reglas Rego se prueba aparte con `opa test`
 * (opa/policies/authz_test.rego) y en el workflow opa.yml.
 */
class OpaAuthorizationClientTest {

    @Test
    void buildInput_armaElDocumentoQueEsperaLaPolitica() {
        Map<String, Object> input = OpaAuthorizationClient.buildInput(
                "u1", List.of("ADMIN"), "refund",
                Map.of("type", "order", "amount", 800));

        @SuppressWarnings("unchecked")
        Map<String, Object> subject = (Map<String, Object>) input.get("subject");
        assertThat(subject.get("id")).isEqualTo("u1");
        assertThat(subject.get("roles")).isEqualTo(List.of("ADMIN"));
        assertThat(input.get("action")).isEqualTo("refund");
        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) input.get("resource");
        assertThat(resource.get("amount")).isEqualTo(800);
    }

    @Test
    void buildInput_toleraNulos() {
        Map<String, Object> input = OpaAuthorizationClient.buildInput(null, null, null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> subject = (Map<String, Object>) input.get("subject");
        assertThat(subject.get("id")).isEqualTo("");
        assertThat((List<?>) subject.get("roles")).isEmpty();
        assertThat(input.get("action")).isEqualTo("");
        assertThat((Map<?, ?>) input.get("resource")).isEmpty();
    }

    @Test
    void shadowMode_deshabilitado_permiteSinLlamarAOpa() {
        // enabled=false -> no hay llamada de red y devuelve true (no bloquea).
        OpaAuthorizationClient client = new OpaAuthorizationClient(
                "http://opa-inexistente:8181", "/v1/data/bookplus/authz/allow",
                false, true);

        boolean allowed = client.isAllowed("u1", List.of("USER"), "refund",
                Map.of("type", "order", "amount", 999999));

        assertThat(allowed).isTrue();
    }

    @Test
    void habilitado_conOpaCaido_yFailOpen_noBloquea() {
        // enabled=true pero OPA no existe -> fail-open=true devuelve true.
        OpaAuthorizationClient client = new OpaAuthorizationClient(
                "http://opa-inexistente.local:1", "/v1/data/bookplus/authz/allow",
                true, true);

        boolean allowed = client.isAllowed("u1", List.of("USER"), "read",
                Map.of("type", "order", "owner", "u1"));

        assertThat(allowed).isTrue();
    }
}
