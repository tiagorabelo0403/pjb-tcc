package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalExternalCertificatesBlueprint {

    private RecursalExternalCertificatesBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.CERTIDOES_NARRATORIA_E_EXECUTIVA,
                RecursalFormalSectionLabels.CERTIDOES_AUTENTICIDADE_E_DOWNLOAD,
                RecursalFormalSectionLabels.CERTIDOES_TRANSITO_RETORNO_EXECUCAO
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("EMITIR_CERTIDAO_NARRATORIA", "emitir certidão narratória profissional ligada ao contexto recursal e ao processo ativo: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.certidaoNarratoriaProfissional(),
                RecursalWorkbenchSurfaceCatalog.advogadoRelacaoProcessos())));
        checklist.put("EMITIR_CERTIDAO_EXECUTIVA", "abrir certidão executiva e de retorno para cumprimento, trânsito ou baixa quando o cenário recursal exigir: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.certidaoExecutivaProfissional(),
                RecursalWorkbenchSurfaceCatalog.processoPrazoReal())));
        checklist.put("PRESERVAR_AUTENTICIDADE_E_DOWNLOAD", "garantir autenticidade, download controlado e trilha de validação da certidão gerada: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.certidaoAutenticidadeProfissional(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaPageResolve())));
        checklist.put("CONECTAR_TRANSITO_E_EXECUCAO", "não tratar a certidão como peça solta; ela precisa conversar com trânsito, retorno executivo e pós-julgamento recursal");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("certidão narratória e certidão executiva não podem ficar restritas ao eixo interno quando o advogado precisa de autos digitais, autenticidade e rastreabilidade no próprio workspace");
        alertas.add("a certidão recursal deve respeitar sigilo, vínculo processual e fase pós-julgamento antes de liberar download ou validação externa");
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "emitir certidões narratória, executiva e de autenticidade no workspace profissional recursal, conectando trânsito, retorno executivo e download validável sem peça solta fora do processo.";
    }
}
