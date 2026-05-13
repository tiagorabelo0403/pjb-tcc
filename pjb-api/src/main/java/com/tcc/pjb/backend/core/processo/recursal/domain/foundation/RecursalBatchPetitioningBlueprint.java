package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalBatchPetitioningBlueprint {

    private RecursalBatchPetitioningBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.PETICIONAMENTO_DISTRIBUICAO_FUTURA_E_LOTE,
                RecursalFormalSectionLabels.ASSINATURA_DIGITAL_EM_LOTE,
                RecursalFormalSectionLabels.PETICIONAMENTO_INTERMEDIARIO_EM_BLOCO,
                RecursalFormalSectionLabels.CONTROLE_DOCUMENTAL_PDF_E_ANEXOS
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("SALVAR_DISTRIBUICAO_FUTURA", "preservar petições iniciais recursais preparadas para distribuição futura antes do protocolo definitivo: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftSave(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoInitialDraftMine(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoDistribuicaoFutura())));
        checklist.put("DISTRIBUIR_EM_LOTE", "permitir distribuição em lote das peças preparadas sem quebrar a governança do protocolo e da competência: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoDistribuicaoLote(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoJourneyInteligente())));
        checklist.put("ASSINAR_EM_LOTE_COM_GOVERNANCA", "concentrar a assinatura digital em lote em uma única espinha operacional, sem espalhar executores paralelos nem retenção indevida de contexto sensível: "
                + RecursalWorkbenchSurfaceCatalog.peticionamentoAssinaturaLote());
        checklist.put("PREPARAR_PETICOES_INTERMEDIARIAS_EM_BLOCO", "reaproveitar a mesma área de trabalho para preparar e peticionar manifestações intermediárias em bloco: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoIntermediarioBloco(),
                RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel())));
        checklist.put("FILTRAR_PDF_E_ANEXOS_COM_GOVERNANCA", "manter filtragem de anexos vazios, PDFs governados, diff de minuta e revisão antes do envio em lote: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioGovernedReview(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioDraftDiff(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioWorkspace())));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "peticionamento em lote e assinatura em lote precisam compartilhar a mesma espinha de protocolo, sem executor satélite e sem lote opaco fora do workspace profissional",
                "distribuição futura e petições intermediárias preparadas devem permanecer visíveis na área de trabalho do advogado até conclusão ou descarte governado"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar distribuição futura, peticionamento em lote, assinatura digital em lote e petições intermediárias em bloco no mesmo eixo profissional do PJB, sem fila paralela nem drift operacional.";
    }
}
