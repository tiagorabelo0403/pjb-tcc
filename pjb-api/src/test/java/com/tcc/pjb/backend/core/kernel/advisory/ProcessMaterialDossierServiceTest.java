package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessMaterialDossierServiceTest {

    private final ProcessMaterialDossierService service = new ProcessMaterialDossierService();

    @Test
    void mustExposeExecutiveSynthesisAndReadinessSignalsForPetitionAssist() {
        LaianePeticaoAssistRequest request = new LaianePeticaoAssistRequest();
        request.setTextoFatosResumido("Plano negou cirurgia urgente e manteve cobrança indevida.");
        request.setAssuntoTpu("Saúde suplementar");
        request.setMateriaPrincipal("Consumidor");
        request.setValorCausa(new BigDecimal("12500"));
        request.setCpfCnpjAutor("12345678901");
        request.setCpfCnpjReu("00987654321000199");
        request.setRequerLiminar(true);
        request.setDocumentosAnexados(List.of("Contrato do plano", "Laudo médico", "Print de WhatsApp", "Comprovante de pagamento"));
        HashMap<String, Object> ctx = new HashMap<>();
        ctx.put("pedidos", List.of("Obrigação de fazer para autorizar cirurgia", "Indenização por danos morais"));
        ctx.put("provas", List.of("Laudo médico atualizado", "Contrato do plano", "Notificação extrajudicial"));
        ctx.put("pedido_principal", "Autorizar a cirurgia imediatamente");
        ctx.put("objeto_processual", "Cobertura contratual de cirurgia urgente");
        request.setCtx(ctx);

        ProcessMaterialDossierReport report = service.analyzeRequest(request, null, "COMUM");

        assertEquals("PETITION_ASSIST", report.lane());
        assertEquals("COMUM", report.diagnostics().get("rito"));
        assertNotNull(report.diagnostics().get("executiveSummary"));
        assertNotNull(report.diagnostics().get("strategicFocus"));
        assertTrue(((Integer) report.diagnostics().get("dossierReadinessScore")) > 0);
        assertTrue(List.of("CRITICA", "ATIVA", "ESTAVEL").contains(report.diagnostics().get("attentionBand")));
        assertFalse(report.evidenceAnchors().isEmpty());
        assertFalse(report.protocolChecklist().isEmpty());
    }

    @Test
    void mustPreserveRiskSignalsInsideProtocolChecklistForProcessAnalysis() {
        Processo processo = new Processo();
        processo.setFaseAtual(com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.INICIAL);
        processo.setObjetoProcessual("Cobrança contratual");
        processo.setPedidoPrincipal("Condenação ao pagamento");
        processo.setAssunto("Cobrança e inadimplemento");
        processo.setPedidosConsolidados("Pagamento integral\nMulta contratual");
        processo.setMaterialProbatorioResumo("Contrato assinado; boleto vencido; comprovante parcial");
        processo.setMaterialProbatorioScore(78);
        processo.setPotencialAcordoScore(66);
        processo.setValorCausa(new BigDecimal("9800"));
        processo.setParteAutoraCpf("12345678901");
        processo.setParteReuCpf("10987654321");

        ProcessMaterialDossierReport report = service.analyzeProcess(processo, List.of("Checar prevenção antes do protocolo", "Confirmar memória do valor da causa"));

        assertEquals("PROCESS", report.lane());
        assertTrue(report.protocolChecklist().stream().anyMatch(item -> item.toLowerCase().contains("preven")));
        assertEquals("ATIVA", report.diagnostics().get("attentionBand"));
        assertTrue(report.diagnostics().containsKey("executiveSummary"));
        assertTrue(((String) report.diagnostics().get("executiveSummary")).contains("Cobrança contratual") || ((String) report.diagnostics().get("executiveSummary")).contains("controvérsia contratual"));
    }
}
