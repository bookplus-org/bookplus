package com.bookplus.order.domain.audit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del registro de auditoría a prueba de manipulación:
 *  - una cadena recién construida es íntegra,
 *  - el primer eslabón enlaza con el hash génesis,
 *  - alterar el contenido de una entrada rompe la verificación,
 *  - borrar una entrada (romper la secuencia) también se detecta.
 */
class HashChainedAuditLogTest {

    @Test
    void cadenaRecienConstruida_esIntegra() {
        HashChainedAuditLog log = new HashChainedAuditLog();
        log.append("admin", "REFUND_APPROVED", "orderId=1 amount=500");
        log.append("system", "STOCK_RESTOCKED", "bookId=A qty=3");
        log.append("admin", "ORDER_CANCELLED", "orderId=2");

        List<AuditEntry> chain = log.entries();
        assertThat(chain).hasSize(3);
        assertThat(chain.get(0).prevHash()).isEqualTo(HashChainedAuditLog.GENESIS);
        assertThat(chain.get(1).prevHash()).isEqualTo(chain.get(0).hash());
        assertThat(HashChainedAuditLog.verify(chain)).isTrue();
    }

    @Test
    void alterarUnaEntrada_rompeLaVerificacion() {
        HashChainedAuditLog log = new HashChainedAuditLog();
        log.append("admin", "REFUND_APPROVED", "orderId=1 amount=500");
        log.append("admin", "REFUND_APPROVED", "orderId=2 amount=999999"); // fraude
        List<AuditEntry> chain = new ArrayList<>(log.entries());

        // Un atacante cambia el importe de la 2ª entrada pero conserva su hash antiguo.
        AuditEntry tampered = new AuditEntry(
                chain.get(1).seq(), chain.get(1).timestampMillis(),
                chain.get(1).actor(), chain.get(1).action(),
                "orderId=2 amount=1",                 // detalle manipulado
                chain.get(1).prevHash(), chain.get(1).hash());
        chain.set(1, tampered);

        assertThat(HashChainedAuditLog.verify(chain)).isFalse();
    }

    @Test
    void borrarUnaEntrada_rompeLaSecuencia() {
        HashChainedAuditLog log = new HashChainedAuditLog();
        log.append("a", "X", "1");
        log.append("b", "Y", "2");
        log.append("c", "Z", "3");
        List<AuditEntry> chain = new ArrayList<>(log.entries());

        chain.remove(1); // eliminar la del medio deja seq inconsistente y enlace roto
        assertThat(HashChainedAuditLog.verify(chain)).isFalse();
    }
}
