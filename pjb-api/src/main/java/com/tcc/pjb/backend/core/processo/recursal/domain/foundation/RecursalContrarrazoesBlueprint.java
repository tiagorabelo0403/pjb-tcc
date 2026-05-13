package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.ArrayList;
import java.util.List;

public final class RecursalContrarrazoesBlueprint {

    private RecursalContrarrazoesBlueprint() {
    }

    public static List<String> secoes(boolean admiteAdesivo) {
        List<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.CONTRARRAZOES);
        sections.add(RecursalFormalSectionLabels.IMPUGNACAO_ESPECIFICA_RECURSO_PRINCIPAL);
        sections.add(RecursalFormalSectionLabels.PEDIDO_NAO_CONHECIMENTO);
        sections.add(RecursalFormalSectionLabels.PEDIDO_NAO_PROVIMENTO);
        if (admiteAdesivo) {
            sections.add(RecursalFormalSectionLabels.SUCUMBENCIA_RECIPROCA);
            sections.add(RecursalFormalSectionLabels.SUBORDINACAO_RECURSO_PRINCIPAL);
        }
        return List.copyOf(sections);
    }

    public static List<String> passos(boolean admiteAdesivo) {
        List<String> steps = new ArrayList<>();
        steps.add("CONFIRMAR_INTIMACAO_PARA_CONTRARRAZOES");
        steps.add("IMPUGNAR_FUNDAMENTOS_DO_RECURSO_PRINCIPAL");
        steps.add("PEDIR_NAO_CONHECIMENTO_OU_NAO_PROVIMENTO");
        if (admiteAdesivo) {
            steps.add("ACOPLAR_RECURSO_ADESIVO_NA_MESMA_JANELA");
        }
        return List.copyOf(steps);
    }
}
