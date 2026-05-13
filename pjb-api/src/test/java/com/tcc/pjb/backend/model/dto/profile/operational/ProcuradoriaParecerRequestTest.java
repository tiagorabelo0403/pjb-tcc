package com.tcc.pjb.backend.model.dto.profile.operational;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcuradoriaParecerRequestTest {

    @Test
    void shouldSanitizeCollectionsAndFlags() {
        ProcuradoriaParecerRequest request = new ProcuradoriaParecerRequest(
                "parecer",
                "fundamentacao",
                java.util.Arrays.asList(
                        PeticionamentoMediaBlocoRequest.builder().tipo("IMAGEM").ancora("a").titulo("ok").build(),
                        null
                ),
                java.util.Arrays.asList("doc-1", "  doc-1  ", null, "doc-2"),
                null,
                List.of("rep-1", " "),
                List.of(),
                Boolean.TRUE,
                Boolean.TRUE
        );

        assertThat(request.midiaInline()).hasSize(1);
        assertThat(request.provasDocumentais()).containsExactly("doc-1", "doc-2");
        assertThat(request.documentosRepresentacao()).containsExactly("rep-1");
        assertThat(request.prepararPacoteProtocoloResolvido()).isTrue();
        assertThat(request.sigiloSensivelResolvido()).isTrue();
    }
}
