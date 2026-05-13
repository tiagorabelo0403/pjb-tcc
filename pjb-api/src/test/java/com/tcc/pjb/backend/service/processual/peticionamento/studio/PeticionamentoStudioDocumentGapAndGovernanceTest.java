package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioDocumentGapAndGovernanceTest {

    @Test
    void deveMarcarLacunaCriticaQuandoRecursoNaoTemDecisaoNemCiencia() {
        PeticionamentoStudioDocumentGapService service = new PeticionamentoStudioDocumentGapService();
        var result = service.build(new PeticionamentoStudioDocumentGapService.ResolveRequest(
                "RECURSAL",
                "APELACAO",
                false,
                true,
                2,
                true,
                true,
                false,
                false,
                false,
                List.of(),
                null,
                null
        ));

        assertEquals("CRITICAL", result.overallStatus());
        assertTrue(result.items().stream().anyMatch(item -> "DECISAO_RECORRIDA".equals(item.get("code")) && "MISSING".equals(item.get("status"))));
        assertTrue(result.items().stream().anyMatch(item -> "CIENCIA_INTIMACAO".equals(item.get("code")) && "MISSING".equals(item.get("status"))));
    }

    @Test
    void deveExigirRevisaoPatronalQuandoModoEscritorioAtivoESemAceite() {
        PeticionamentoStudioGovernedReviewService service = new PeticionamentoStudioGovernedReviewService();
        var result = service.build(new PeticionamentoStudioGovernedReviewService.ResolveRequest(
                "PETICAO_BASE",
                "ADVOGADO_MODO_ESCRITORIO",
                "RAPIDO_ASSISTIDO",
                "ESCRITORIO_PATRONAL",
                "Dra. Patrono",
                "Escritório PJB",
                true,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals("ESCRITORIO_PATRONAL", result.governanceMode());
        assertFalse(result.blockers().isEmpty());
        assertTrue(result.lanes().stream().anyMatch(item -> "REVISAO_PATRONAL".equals(item.get("code"))));
    }
}
