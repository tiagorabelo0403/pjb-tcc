
package com.tcc.pjb.backend.service.processual.peticionamento;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoDocumentBatchReadingStrategyServiceTest {

    @Test
    void deveClassificarLotePriorizarPecaBaseERepresentacao() {
        PeticionamentoDocumentBatchReadingStrategyService service = new PeticionamentoDocumentBatchReadingStrategyService();

        PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport report = service.plan(
                new PeticionamentoDocumentBatchReadingStrategyService.ResolveRequest(
                        "Ação de alimentos com urgência",
                        "FAMILIA",
                        "COMUM",
                        "ALIMENTOS",
                        "ESTADUAL",
                        "Alimentos",
                        "Direito de família",
                        "Minuta da inicial",
                        null,
                        "Menor precisa de alimentos provisórios.",
                        List.of("peticao_inicial.pdf", "procuracao_assinada.pdf", "certidao_nascimento.pdf", "contracheque.pdf"),
                        true,
                        true,
                        true,
                        true,
                        false
                )
        );

        assertEquals("PETICIONAMENTO_BATCH_LEITURA_GUARDADA_V2", report.profile());
        assertFalse(report.blocking());
        assertTrue(report.mandatorySequence().stream().anyMatch(item -> item.contains("Núcleo da peça")));
        assertTrue(report.orderedDocuments().stream().anyMatch(item -> "REPRESENTACAO".equals(item.get("category"))));
        assertTrue(report.orderedDocuments().stream().anyMatch(item -> "PECA_BASE".equals(item.get("category"))));
    }

    @Test
    void deveBloquearQuandoRepresentacaoFormalNaoFoiAnexada() {
        PeticionamentoDocumentBatchReadingStrategyService service = new PeticionamentoDocumentBatchReadingStrategyService();

        PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport report = service.plan(
                new PeticionamentoDocumentBatchReadingStrategyService.ResolveRequest(
                        "Mandado de segurança",
                        "PUBLICO",
                        "ESPECIAL",
                        "MANDADO_DE_SEGURANCA",
                        "FEDERAL",
                        "MS",
                        "Mandado de segurança",
                        null,
                        "Peça base em texto puro",
                        null,
                        List.of("ato_coator.pdf", "documento_identidade.pdf"),
                        false,
                        false,
                        true,
                        true,
                        false
                )
        );

        assertTrue(report.blocking());
        assertTrue(report.blockingIssues().stream().anyMatch(item -> item.contains("instrumento formal de representação")));
    }
}
