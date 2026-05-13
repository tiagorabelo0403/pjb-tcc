
package com.tcc.pjb.backend.service.processual.peticionamento.media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeticionamentoMediaSecurityPipelineServiceTest {

    private final PeticionamentoMediaSecurityPipelineService service = new PeticionamentoMediaSecurityPipelineService();

    @Test
    void deveBloquearMidiaSemMetadadoMinimoOuBlurJudicial() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("video")
                .categoria("midia_sensivel")
                .mimeType("video/mp4")
                .sensivelAdultoDeclarado(true)
                .exigirBlurInicial(true)
                .magistradoPodeDesborrar(false)
                .build();

        var report = service.assess(new PeticionamentoMediaSecurityPipelineService.ResolveRequest(List.of(block), null, true));

        assertTrue(report.blocking());
        assertTrue(report.blockers().stream().anyMatch(v -> v.contains("metadado mínimo")));
        assertTrue(report.blockers().stream().anyMatch(v -> v.contains("magistrado")));
    }

    @Test
    void deveAceitarImagemCanonicaComMetadadoMinimo() {
        PeticionamentoMediaBlocoRequest block = PeticionamentoMediaBlocoRequest.builder()
                .tipo("imagem")
                .mimeType("image/jpeg")
                .storageKey("peticoes/abc.jpg")
                .hashSha384("a".repeat(96))
                .tamanhoBytes(1024L)
                .build();

        var report = service.assess(new PeticionamentoMediaSecurityPipelineService.ResolveRequest(List.of(block), null, false));

        assertFalse(report.blocking());
        assertTrue(report.workspace().containsKey("tripleShield"));
    }
}
