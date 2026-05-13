package com.tcc.pjb.backend.service.processual.peticionamento;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoPericiaEvidenceIntelligenceServiceTest {

    private final PeticionamentoPericiaEvidenceIntelligenceService service = new PeticionamentoPericiaEvidenceIntelligenceService();

    @Test
    void deveSugerirPericiaAudiovisualECustodiaReforcada() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("video")
                .categoria("midia_sensivel")
                .mimeType("video/mp4")
                .storageKey("peticoes/video.mp4")
                .hashSha384("a".repeat(96))
                .tamanhoBytes(12_345L)
                .duracaoMs(120_000L)
                .sensivelAdultoDeclarado(true)
                .build();

        var report = service.analyze(new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(
                List.of(block),
                List.of("laudo.pdf"),
                true
        ));

        assertTrue(report.workspace().toString().contains("AUDIOVISUAL_FORENSE"));
        assertTrue(report.workspace().toString().contains("REFORCADA"));
    }
}
