package com.bookplus.order.shared.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el cifrado de PII (AES-GCM) del CryptoConverter, sin Spring ni base de datos.
 */
class CryptoConverterTest {

    private final CryptoConverter converter = new CryptoConverter();

    @Test
    void cifra_y_descifra_ida_y_vuelta() {
        String original = "Ada Lovelace, 1 Analytical Engine St";

        String cipher = converter.convertToDatabaseColumn(original);

        assertThat(cipher).isNotNull().isNotEqualTo(original);   // no se guarda en claro
        assertThat(converter.convertToEntityAttribute(cipher)).isEqualTo(original);
    }

    @Test
    void el_mismo_valor_produce_textos_cifrados_distintos() {
        // IV aleatorio por cifrado -> dos ciphertexts diferentes para el mismo dato.
        String a = converter.convertToDatabaseColumn("dato sensible");
        String b = converter.convertToDatabaseColumn("dato sensible");

        assertThat(a).isNotEqualTo(b);
        assertThat(converter.convertToEntityAttribute(a)).isEqualTo("dato sensible");
        assertThat(converter.convertToEntityAttribute(b)).isEqualTo("dato sensible");
    }

    @Test
    void null_se_mantiene_como_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void un_texto_manipulado_falla_al_descifrar() {
        // GCM es cifrado autenticado: detecta manipulación del dato en reposo.
        assertThatThrownBy(() -> converter.convertToEntityAttribute("no-es-base64-valido$$$"))
                .isInstanceOf(IllegalStateException.class);
    }
}
