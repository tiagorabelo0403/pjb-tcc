package com.tcc.pjb.backend.service.processual.peticionamento.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;

class PeticionamentoMediaPublicationGateServiceTest {

    private final PeticionamentoMediaPublicationGateService service = new PeticionamentoMediaPublicationGateService();

    @Test
    void deveBloquearMidiaSemVinculoMinimoDeUpload() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("imagem")
                .ancora("foto-fato")
                .mimeType("image/jpeg")
                .tamanhoBytes(1024L)
                .build();

        var multimedia = new PeticionamentoMultimidiaComposerService().compose(
                new PeticionamentoMultimidiaComposerService.ResolveRequest(List.of(block), List.of(), List.of(), List.of(), List.of())
        );
        var security = new PeticionamentoMediaSecurityPipelineService().assess(
                new PeticionamentoMediaSecurityPipelineService.ResolveRequest(List.of(block), null, false)
        );
        var storage = new PeticionamentoMediaStorageShieldService().plan(
                new PeticionamentoMediaStorageShieldService.ResolveRequest(List.of(block), List.of(), List.of(), List.of(), List.of(), true)
        );
        var pericia = new PeticionamentoPericiaEvidenceIntelligenceService().analyze(
                new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(List.of(block), List.of(), false)
        );

        var report = service.resolve(new PeticionamentoMediaPublicationGateService.ResolveRequest(
                List.of(block),
                multimedia,
                security,
                storage,
                pericia,
                true
        ));

        assertTrue(report.blocking());
        assertEquals("PUBLICACAO_CONTROLADA_BLOQUEADA", report.profile());
        var first = ((List<Map<String, Object>>) report.workspace().get("fileStates")).get(0);
        assertEquals("BLOQUEADO", first.get("publicationState"));
    }

    @Test
    void deveManterVideoEmTriploOkAteCanonicalizacaoEPericia() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("video")
                .ancora("video-prova")
                .mimeType("video/mp4")
                .storageKey("peticoes/video-prova.mp4")
                .uploadItemId("up-1")
                .hashSha384("a".repeat(96))
                .tamanhoBytes(12_000_000L)
                .duracaoMs(120_000L)
                .build();

        var multimedia = new PeticionamentoMultimidiaComposerService().compose(
                new PeticionamentoMultimidiaComposerService.ResolveRequest(List.of(block), List.of("contrato.pdf"), List.of(), List.of(), List.of())
        );
        var security = new PeticionamentoMediaSecurityPipelineService().assess(
                new PeticionamentoMediaSecurityPipelineService.ResolveRequest(List.of(block), null, false)
        );
        var storage = new PeticionamentoMediaStorageShieldService().plan(
                new PeticionamentoMediaStorageShieldService.ResolveRequest(List.of(block), List.of("contrato.pdf"), List.of(), List.of(), List.of(), true)
        );
        var pericia = new PeticionamentoPericiaEvidenceIntelligenceService().analyze(
                new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(List.of(block), List.of("contrato.pdf"), false)
        );

        var report = service.resolve(new PeticionamentoMediaPublicationGateService.ResolveRequest(
                List.of(block),
                multimedia,
                security,
                storage,
                pericia,
                true
        ));

        assertFalse(report.blocking());
        assertTrue(report.pendingPublication());
        var first = ((List<Map<String, Object>>) report.workspace().get("fileStates")).get(0);
        assertEquals("AGUARDANDO_TRIPLO_OK", first.get("publicationState"));
        assertEquals("CONDICIONADO_A_TRIPLO_OK", first.get("protocolGate"));
    }
}
