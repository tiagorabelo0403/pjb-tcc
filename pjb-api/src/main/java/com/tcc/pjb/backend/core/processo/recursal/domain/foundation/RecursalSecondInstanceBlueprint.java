package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.List;

public final class RecursalSecondInstanceBlueprint {

    private RecursalSecondInstanceBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        List<String> sections = new ArrayList<>();
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            sections.add(RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO);
        } else {
            sections.add(RecursalFormalSectionLabels.DISTRIBUICAO_TRIBUNAL);
            sections.add(RecursalFormalSectionLabels.RELATORIA);
        }
        sections.add(RecursalFormalSectionLabels.PAUTA_JULGAMENTO);
        if (request.desejaSustentacaoOral()) {
            sections.add(RecursalFormalSectionLabels.SUSTENTACAO_ORAL);
            sections.add(RecursalFormalSectionLabels.PEDIDO_SUSTENTACAO_ORAL);
        }
        sections.add(RecursalFormalSectionLabels.JULGAMENTO_COLEGIADO);
        sections.add(RecursalFormalSectionLabels.PUBLICACAO_ACORDAO);
        if (rotaExigeSubidaEstrita(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO);
            sections.add(RecursalFormalSectionLabels.SUBIDA_CORTE_SUPERIOR);
        }
        return List.copyOf(sections);
    }

    public static List<String> passos(String recursoPrincipal, RecursalAutomationRequest request) {
        List<String> steps = new ArrayList<>();
        steps.add(RecursalSecondInstancePhase.REGISTRO_NO_PROTOCOLO_TRIBUNAL.name());
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            steps.add(RecursalSecondInstancePhase.DISTRIBUICAO.name());
            steps.add(RecursalSecondInstancePhase.CONCLUSAO_RELATOR.name());
        } else {
            steps.add(RecursalSecondInstancePhase.DISTRIBUICAO.name());
            steps.add(RecursalSecondInstancePhase.CONCLUSAO_RELATOR.name());
        }
        steps.add(RecursalSecondInstancePhase.PAUTA_PUBLICADA.name());
        if (request.desejaSustentacaoOral()) {
            steps.add(RecursalSecondInstancePhase.SUSTENTACAO_ORAL.name());
        }
        steps.add(RecursalSecondInstancePhase.JULGAMENTO_COLEGIADO.name());
        steps.add(RecursalSecondInstancePhase.PUBLICACAO_ACORDAO.name());
        return List.copyOf(steps);
    }

    public static boolean rotaExigeSubidaEstrita(String recursoPrincipal) {
        return switch (recursoPrincipal) {
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO" -> true;
            default -> false;
        };
    }
}
