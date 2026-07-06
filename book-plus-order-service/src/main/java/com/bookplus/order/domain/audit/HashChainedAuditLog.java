package com.bookplus.order.domain.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Registro de auditoría append-only y a prueba de manipulación (tamper-evident).
 *
 * Cada entrada se encadena a la anterior mediante SHA-256:
 *   hash(n) = SHA256( seq | ts | actor | action | details | hash(n-1) )
 * La primera entrada enlaza con un hash "génesis". {@link #verify} recomputa toda
 * la cadena; si alguien altera, inserta o borra una entrada, el enlace se rompe y
 * la verificación falla. Es un requisito típico de auditoría/cumplimiento en banca.
 */
public final class HashChainedAuditLog {

    /** Hash génesis (no hay entrada anterior a la primera). */
    public static final String GENESIS = "0".repeat(64);

    private final List<AuditEntry> entries = new ArrayList<>();

    /** Añade una entrada firmada por la cadena y la devuelve. */
    public AuditEntry append(String actor, String action, String details) {
        long seq = entries.size();
        long ts = Instant.now().toEpochMilli();
        String prev = entries.isEmpty() ? GENESIS : entries.get(entries.size() - 1).hash();
        String hash = computeHash(seq, ts, actor, action, details, prev);
        AuditEntry entry = new AuditEntry(seq, ts, actor, action, details, prev, hash);
        entries.add(entry);
        return entry;
    }

    /** Copia inmutable de la cadena actual. */
    public List<AuditEntry> entries() {
        return List.copyOf(entries);
    }

    // ── Verificación de integridad ────────────────────────────────────────────

    /**
     * Recomputa la cadena entera y comprueba: secuencia correcta, enlace prevHash
     * correcto y hash de cada entrada coherente con su contenido.
     *
     * @return true si la cadena es íntegra; false si detecta manipulación.
     */
    public static boolean verify(List<AuditEntry> chain) {
        String expectedPrev = GENESIS;
        for (int i = 0; i < chain.size(); i++) {
            AuditEntry e = chain.get(i);
            if (e.seq() != i) return false;
            if (!expectedPrev.equals(e.prevHash())) return false;
            String recomputed = computeHash(e.seq(), e.timestampMillis(),
                    e.actor(), e.action(), e.details(), e.prevHash());
            if (!recomputed.equals(e.hash())) return false;
            expectedPrev = e.hash();
        }
        return true;
    }

    static String computeHash(long seq, long ts, String actor, String action,
                              String details, String prevHash) {
        String payload = seq + "|" + ts + "|" + nz(actor) + "|" + nz(action)
                + "|" + nz(details) + "|" + prevHash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
