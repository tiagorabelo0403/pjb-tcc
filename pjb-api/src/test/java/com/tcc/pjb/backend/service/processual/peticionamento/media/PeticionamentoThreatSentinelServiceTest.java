package com.tcc.pjb.backend.service.processual.peticionamento.media;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoThreatSentinelServiceTest {

    private final PeticionamentoThreatSentinelService service = new PeticionamentoThreatSentinelService();

    @Test
    void devePlanejarSentinelaSensivel() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("video")
                .sensivelAdultoDeclarado(true)
                .exigirBlurInicial(true)
                .magistradoPodeDesborrar(true)
                .storageKey("peticoes/video-1.mp4")
                .build();

        var report = service.plan(new PeticionamentoThreatSentinelService.ResolveRequest("sessao-1", List.of(block), true));

        assertTrue(report.profile().contains("SENSIVEL"));
        assertTrue(report.watchSignals().stream().anyMatch(v -> v.contains("VISUALIZACAO")));
    }
}
