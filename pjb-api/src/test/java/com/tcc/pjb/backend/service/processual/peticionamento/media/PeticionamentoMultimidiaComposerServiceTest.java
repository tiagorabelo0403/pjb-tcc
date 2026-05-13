package com.tcc.pjb.backend.service.processual.peticionamento.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoMultimidiaComposerServiceTest {

    private final PeticionamentoMultimidiaComposerService service = new PeticionamentoMultimidiaComposerService();

    @Test
    void deveMontarPeticaoMultimidiaComSecoesSeparadas() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("audio")
                .titulo("Áudio da negociação")
                .descricao("Trecho principal da conversa")
                .storageKey("peticoes/audio-1.mp3")
                .build();

        var report = service.compose(new PeticionamentoMultimidiaComposerService.ResolveRequest(
                List.of(block),
                List.of("contrato.pdf"),
                List.of("rg.pdf"),
                List.of("procuracao.pdf"),
                List.of("comprovante.pdf")
        ));

        assertTrue(report.enabled());
        assertEquals("MULTIMIDIA_NARRATIVA_EXPANDIDA", report.profile());
        assertTrue(report.workspace().containsKey("sections"));
        assertTrue(report.workspace().toString().contains("postPetitionAttachmentBlock"));
    }

    @Test
    void deveBloquearDocumentoInline() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("documento")
                .titulo("Contrato")
                .storageKey("peticoes/contrato.pdf")
                .build();

        var report = service.compose(new PeticionamentoMultimidiaComposerService.ResolveRequest(
                List.of(block),
                List.of("contrato.pdf"),
                List.of(),
                List.of(),
                List.of()
        ));

        assertTrue(report.blocking());
        assertTrue(report.blockers().stream().anyMatch(v -> v.contains("bloco pós-petição")));
    }
}
