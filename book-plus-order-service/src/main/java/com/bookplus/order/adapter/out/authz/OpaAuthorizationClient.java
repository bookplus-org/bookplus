package com.bookplus.order.adapter.out.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Punto de aplicación (PEP) de autorización contra Open Policy Agent.
 *
 * En vez de codificar reglas de "quién puede hacer qué" dentro del servicio,
 * este cliente envía a OPA un input {sujeto, acción, recurso} y OPA responde
 * allow/deny evaluando las políticas Rego (opa/policies/authz.rego). La lógica
 * de autorización queda así desacoplada, versionada y testeada aparte.
 *
 * Modo de despliegue seguro:
 *  - {@code opa.enabled=false} (por defecto): "shadow mode", no bloquea nada;
 *    útil para desplegar y observar antes de aplicar (rollout gradual).
 *  - {@code opa.fail-open=true}: si OPA no responde, no se bloquea al usuario
 *    (evita caídas por indisponibilidad de OPA). En un entorno de alta seguridad
 *    se pondría fail-open=false (denegar ante duda).
 */
@Component
public class OpaAuthorizationClient {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthorizationClient.class);

    private final RestClient restClient;
    private final String decisionPath;
    private final boolean enabled;
    private final boolean failOpen;

    public OpaAuthorizationClient(
            @Value("${opa.url:http://localhost:8181}") String opaUrl,
            @Value("${opa.decision-path:/v1/data/bookplus/authz/allow}") String decisionPath,
            @Value("${opa.enabled:false}") boolean enabled,
            @Value("${opa.fail-open:true}") boolean failOpen) {
        this.restClient = RestClient.builder().baseUrl(opaUrl).build();
        this.decisionPath = decisionPath;
        this.enabled = enabled;
        this.failOpen = failOpen;
    }

    /**
     * Decide si {@code subjectId} (con sus {@code roles}) puede ejecutar
     * {@code action} sobre {@code resource}. Devuelve true/false.
     */
    public boolean isAllowed(String subjectId, Collection<String> roles,
                             String action, Map<String, Object> resource) {
        if (!enabled) {
            return true; // shadow mode: no se aplica autorización externa
        }
        Map<String, Object> body = Map.of("input", buildInput(subjectId, roles, action, resource));
        try {
            OpaResponse resp = restClient.post()
                    .uri(decisionPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OpaResponse.class);
            boolean allow = resp != null && Boolean.TRUE.equals(resp.result());
            if (!allow) {
                log.info("OPA denegó acción '{}' del sujeto '{}' sobre recurso {}",
                        action, subjectId, resource);
            }
            return allow;
        } catch (Exception ex) {
            log.warn("OPA no disponible ({}). Aplicando fail-open={}", ex.getMessage(), failOpen);
            return failOpen;
        }
    }

    /** Construye el documento de entrada que espera la política (función pura, testeable). */
    static Map<String, Object> buildInput(String subjectId, Collection<String> roles,
                                          String action, Map<String, Object> resource) {
        return Map.of(
                "subject", Map.of(
                        "id", subjectId == null ? "" : subjectId,
                        "roles", roles == null ? List.of() : List.copyOf(roles)),
                "action", action == null ? "" : action,
                "resource", resource == null ? Map.of() : resource);
    }

    /** Respuesta de la Data API de OPA: {"result": true|false}. */
    record OpaResponse(Boolean result) {
    }
}
