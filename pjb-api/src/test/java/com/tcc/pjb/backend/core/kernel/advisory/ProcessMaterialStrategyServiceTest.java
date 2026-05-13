package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessMaterialStrategyServiceTest {

    private final ProcessMaterialStrategyService service = new ProcessMaterialStrategyService();
    private final ProcessMaterialDossierService dossierService = new ProcessMaterialDossierService();

    @Test
    void mustCentralizeStrategyMessagesAndProduceReadableRequestStrategy() {
        LaianePeticaoAssistRequest request = new LaianePeticaoAssistRequest();
        request.setTextoFatosResumido("Plano de saúde negou cirurgia urgente e manteve cobrança indevida.");
        request.setAssuntoTpu("Saúde suplementar");
        request.setMateriaPrincipal("Consumidor");
        request.setValorCausa(new BigDecimal("12500"));
        request.setCpfCnpjAutor("12345678901");
        request.setCpfCnpjReu("00987654321000199");
        request.setCasoUrgente(true);
        request.setRequerJuizadoEspecial(true);
        request.setDocumentosAnexados(List.of("Contrato", "Laudo médico", "Print de WhatsApp"));
        HashMap<String, Object> ctx = new HashMap<>();
        ctx.put("pedidos", List.of("Autorizar cirurgia", "Indenização por danos morais"));
        ctx.put("pedido_principal", "Autorizar cirurgia imediatamente");
        ctx.put("objeto_processual", "Cobertura contratual de cirurgia urgente");
        request.setCtx(ctx);

        ProcessMaterialDossierReport dossier = dossierService.analyzeRequest(request, null, "COMUM");
        ProcessMaterialStrategyReport report = service.analyzeRequest(request, null, "COMUM", dossier, 0.83d, List.of("priorizar fluxo de liminar"));

        assertEquals("APTO_SUPERVISIONADO", report.protocolReadiness());
        assertFalse(report.pleadingBlueprint().isEmpty());
        assertFalse(report.executionChecklist().isEmpty());
        assertTrue(report.controlPoints().stream().anyMatch(item -> item.contains("Sinal operacional:")));
        assertTrue(report.metrics().containsKey("readinessScore"));
    }

    @Test
    void mustBlockWeakProcessWithMissingCoreSignals() {
        Processo processo = new Processo();
        processo.setObjetoProcessual("Cobrança contratual");
        processo.setPedidoPrincipal("Condenação ao pagamento");
        processo.setMaterialProbatorioResumo("Print isolado sem contrato");
        processo.setMaterialProbatorioScore(32);
        processo.setPotencialAcordoScore(41);
        processo.setValorCausa(BigDecimal.ZERO);

        ProcessMaterialDossierReport dossier = dossierService.analyzeProcess(processo, List.of("checar prevenção"));
        ProcessMaterialStrategyReport report = service.analyzeProcess(processo, dossier, List.of("faltam anexos essenciais"));

        assertEquals("BLOQUEADO", report.protocolReadiness());
        assertTrue(report.protocolBlockers().stream().anyMatch(item -> item.contains("Valor da causa ausente") || item.contains("Lastro probatório insuficiente")));
        assertTrue(report.negotiationGuardrails().stream().anyMatch(item -> item.contains("postura cautelosa") || item.contains("narrativa contenciosa")));
    }
}
