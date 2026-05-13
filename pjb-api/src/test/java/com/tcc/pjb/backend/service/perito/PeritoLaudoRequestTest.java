package com.tcc.pjb.backend.service.perito;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoLaudoRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeritoLaudoRequestTest {

    @Test
    void shouldSanitizeOptionalCollections() {
        PeritoLaudoRequest request = new PeritoLaudoRequest(
                "DIGITAL",
                "Conclusão",
                "Metodologia",
                Arrays.asList(PeticionamentoMediaBlocoRequest.builder().tipo("IMAGEM").ancora("img").build(), null),
                List.of(" doc-1 ", "", "doc-1"),
                null,
                List.of(" procuração "),
                null,
                Boolean.TRUE
        );

        assertThat(request.midiaInline()).hasSize(1);
        assertThat(request.provasDocumentais()).containsExactly("doc-1");
        assertThat(request.documentosRepresentacao()).containsExactly("procuração");
        assertThat(request.prepararPacoteProtocoloResolvido()).isTrue();
    }
}
