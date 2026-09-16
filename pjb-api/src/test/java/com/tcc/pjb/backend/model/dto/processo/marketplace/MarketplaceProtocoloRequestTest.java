package com.tcc.pjb.backend.model.dto.processo.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.Attachment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MarketplaceProtocoloRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<Attachment> anexos(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> Attachment.builder().name("doc" + i + ".pdf").build())
                .toList();
    }

    private static MarketplaceProtocoloRequest request(List<Attachment> documentos) {
        return new MarketplaceProtocoloRequest(
                "client-ref-1", "0001234-56.2026.8.06.0001", "ESTADUAL", "CIVEL", "CE", "Fortaleza",
                "PROCEDIMENTO_COMUM_CIVEL", "Cobrança", "Condenar ao pagamento", null,
                "Autor Teste", "12345678901", null, null, null,
                "", null, "", null, false, documentos, "ADVOGADO");
    }

    @Test
    void semAnexosPassaValidacao() {
        Set<ConstraintViolation<MarketplaceProtocoloRequest>> violacoes = validator.validate(request(List.of()));
        assertThat(violacoes).isEmpty();
    }

    @Test
    void cincoAnexosPassaValidacao() {
        Set<ConstraintViolation<MarketplaceProtocoloRequest>> violacoes = validator.validate(request(anexos(5)));
        assertThat(violacoes).isEmpty();
    }

    @Test
    void seisAnexosFalhaValidacao() {
        Set<ConstraintViolation<MarketplaceProtocoloRequest>> violacoes = validator.validate(request(anexos(6)));
        assertThat(violacoes).isNotEmpty();
    }
}
