package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioDraftDiffServiceTest {

    @Test
    void deveApontarSecaoNovaNaMinutaConsolidada() {
        PeticionamentoStudioDraftDiffService service = new PeticionamentoStudioDraftDiffService();
        var diff = service.diff(new PeticionamentoStudioDraftDiffService.ResolveRequest(
                "RASCUNHO_ATUAL",
                "# Fatos\n\nTexto base",
                "MINUTA_CONSOLIDADA",
                "# Fatos\n\nTexto base\n\n## Pedidos\n\nPedido novo"
        ));

        assertTrue(diff.sections().stream().anyMatch(item -> "Pedidos".equals(item.get("heading")) && "ADDED".equals(item.get("status"))));
    }
}
