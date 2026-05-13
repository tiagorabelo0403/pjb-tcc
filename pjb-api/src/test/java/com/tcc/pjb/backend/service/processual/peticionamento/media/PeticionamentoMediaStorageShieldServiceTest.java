package com.tcc.pjb.backend.service.processual.peticionamento.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoMediaStorageShieldServiceTest {

    private final PeticionamentoMediaStorageShieldService service = new PeticionamentoMediaStorageShieldService();

    @Test
    void deveBloquearDocumentoInlineNoCorpoDaPeticao() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("documento")
                .titulo("Contrato principal")
                .storageKey("peticoes/contrato.pdf")
                .hashSha384("a".repeat(96))
                .mimeType("application/pdf")
                .tamanhoBytes(1024L)
                .build();

        var report = service.plan(new PeticionamentoMediaStorageShieldService.ResolveRequest(
                List.of(block),
                List.of("contrato.pdf"),
                List.of(),
                List.of(),
                List.of(),
                true
        ));

        assertTrue(report.blocking());
        assertTrue(report.blockers().stream().anyMatch(v -> v.contains("bloco pós-petição")));
    }

    @Test
    void devePlanejarStorageExternoSemBlobNoBanco() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("video")
                .titulo("Vídeo do local")
                .storageKey("peticoes/video.mp4")
                .hashSha384("b".repeat(96))
                .mimeType("video/mp4")
                .tamanhoBytes(4_096L)
                .build();

        var report = service.plan(new PeticionamentoMediaStorageShieldService.ResolveRequest(
                List.of(block),
                List.of("laudo.pdf"),
                List.of("rg.pdf"),
                List.of("procuracao.pdf"),
                List.of("comprovante.pdf"),
                true
        ));

        assertFalse(report.blocking());
        assertTrue(report.workspace().containsKey("persistencePolicy"));
        assertTrue(report.workspace().toString().contains("NAO_PERSISTIR_BLOB_MIDIA_NO_BANCO_RELACIONAL"));
    }
}
