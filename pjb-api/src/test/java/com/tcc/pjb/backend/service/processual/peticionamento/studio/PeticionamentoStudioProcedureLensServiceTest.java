package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioProcedureLensServiceTest {

    @Test
    void deveClassificarApelacaoComoFluxoRecursalAssistido() {
        PeticionamentoStudioProcedureLensService service = new PeticionamentoStudioProcedureLensService();

        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tituloCaso("Apelação cível contra sentença de improcedência")
                .classeProcessual("APELACAO")
                .ramoDireito("CIVIL")
                .ritoProcessual("COMUM_ORDINARIO")
                .ctx(Map.of("appealType", "APELACAO"))
                .build();

        var report = service.resolve(request, Map.of("ramoDireito", "CIVIL", "ritoProcessual", "COMUM_ORDINARIO"), Map.of());

        assertEquals("RECURSAL", report.petitionFamily());
        assertEquals("RECURSAL_TECNICO_ASSISTIDO", report.draftingMode());
        assertEquals("APELACAO", report.appealType().name());
        assertFalse(report.workspace().isEmpty());
        assertTrue(report.checklist().stream().anyMatch(item -> item.contains("decisão") || item.contains("Decisão") || item.contains("ciência")));
    }

    @Test
    void deveClassificarEmbargosDeclaracaoComViciosDetectados() {
        PeticionamentoStudioProcedureLensService service = new PeticionamentoStudioProcedureLensService();

        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tituloCaso("Embargos de declaração por omissão e contradição")
                .classeProcessual("EMBARGOS_DECLARACAO")
                .fundamentosJuridicos(List.of("A decisão incorreu em omissão sobre pedido principal e contradição interna."))
                .ctx(Map.of("embargosGrounds", List.of("omissao", "contradicao")))
                .build();

        var report = service.resolve(request, Map.of("ramoDireito", "CIVIL", "ritoProcessual", "COMUM_ORDINARIO"), Map.of());

        assertEquals("EMBARGOS", report.petitionFamily());
        assertEquals("EMBARGOS_DECLARACAO", report.appealType().name());
        assertTrue(report.embargosGrounds().contains("OMISSAO"));
        assertTrue(report.embargosGrounds().contains("CONTRADICAO"));
        assertTrue(report.alerts().stream().anyMatch(item -> item.contains("Embargos exigem individualização") || item.contains("vicio")) || !report.alerts().isEmpty());
    }
}
