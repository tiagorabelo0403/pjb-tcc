package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioEvidenceSummaryServiceTest {

    @Test
    void deveResumirMidiasEDocumentosSemConfundirMetadadoComLeituraPlenaDoConteudo() {
        PeticionamentoStudioEvidenceSummaryService service = new PeticionamentoStudioEvidenceSummaryService();

        var report = service.summarize(new PeticionamentoStudioEvidenceSummaryService.ResolveRequest(
                List.of(
                        PeticionamentoMediaBlocoRequest.builder()
                                .tipo("IMAGEM")
                                .categoria("PROVA_DOCUMENTAL")
                                .titulo("Foto do dano no veículo")
                                .descricao("Registro do amassado frontal após a colisão")
                                .build(),
                        PeticionamentoMediaBlocoRequest.builder()
                                .tipo("VIDEO")
                                .categoria("MIDIA_SENSIVEL")
                                .titulo("Vídeo da abordagem")
                                .contextoProbatorioSensivel(true)
                                .build()
                ),
                List.of("boletim_de_ocorrencia.pdf"),
                List.of("orcamento_oficina.pdf"),
                List.of("rg_autor.pdf"),
                List.of("procuracao_assinada.pdf"),
                Map.of("profile", "PERICIA_AUDIOVISUAL_CUSTODIA_REFORCADA")
        ));

        assertEquals("DOSSIE_EVIDENCIA_INTELIGENTE_ATIVO", report.profile());
        assertTrue(report.items().stream().anyMatch(item -> "ROTULAGEM_E_METADADOS_ASSISTIDOS".equals(item.get("summaryMode"))));
        assertTrue(report.items().stream().anyMatch(item -> "DOCUMENTO_REPRESENTACAO".equals(item.get("evidenceType"))));
        assertFalse(report.warnings().isEmpty());
        assertEquals("PERICIA_AUDIOVISUAL_CUSTODIA_REFORCADA", ((Map<?, ?>) report.workspace().get("pericialWorkspace")).get("profile"));
    }
}
