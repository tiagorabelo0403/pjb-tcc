package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioProofRequestMatrixServiceTest {

    @Test
    void deveClassificarPedidoComoRobustoQuandoHaFatoFundamentoEProva() {
        PeticionamentoStudioProofRequestMatrixService service = new PeticionamentoStudioProofRequestMatrixService();

        var report = service.build(new PeticionamentoStudioProofRequestMatrixService.ResolveRequest(
                List.of("Condenar a ré a restituir o valor pago."),
                List.of("A ré não restituiu o valor pago pelo autor após a rescisão contratual."),
                List.of("Aplica-se a restituição integral do valor indevidamente retido."),
                List.of(Map.of("label", "comprovante_pagamento.pdf", "summary", "Comprovante do valor pago pelo autor."))
        ));

        assertEquals("ROBUSTO", report.workspace().get("overallStrength"));
        assertTrue(report.items().stream().anyMatch(item -> "ROBUSTO".equals(item.get("strength"))));
    }
}
