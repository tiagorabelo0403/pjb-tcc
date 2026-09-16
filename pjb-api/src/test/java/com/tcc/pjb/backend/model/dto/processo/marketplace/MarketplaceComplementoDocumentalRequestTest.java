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

class MarketplaceComplementoDocumentalRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<Attachment> anexos(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> Attachment.builder().name("doc" + i + ".pdf").build())
                .toList();
    }

    @Test
    void cincoAnexosPassaValidacao() {
        var request = new MarketplaceComplementoDocumentalRequest(anexos(5));
        Set<ConstraintViolation<MarketplaceComplementoDocumentalRequest>> violacoes = validator.validate(request);
        assertThat(violacoes).isEmpty();
    }

    @Test
    void seisAnexosFalhaValidacao() {
        var request = new MarketplaceComplementoDocumentalRequest(anexos(6));
        Set<ConstraintViolation<MarketplaceComplementoDocumentalRequest>> violacoes = validator.validate(request);
        assertThat(violacoes).isNotEmpty();
    }

    @Test
    void listaVaziaFalhaValidacao() {
        var request = new MarketplaceComplementoDocumentalRequest(List.of());
        Set<ConstraintViolation<MarketplaceComplementoDocumentalRequest>> violacoes = validator.validate(request);
        assertThat(violacoes).isNotEmpty();
    }
}
