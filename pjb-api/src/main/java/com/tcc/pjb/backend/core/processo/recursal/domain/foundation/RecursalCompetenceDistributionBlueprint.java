package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalCompetenceDistributionBlueprint {

    private RecursalCompetenceDistributionBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.COMPETENCIA_MULTIDIMENSIONAL_DISTRIBUICAO,
                RecursalFormalSectionLabels.AMBIGUIDADE_COMPETENCIA_ESCOLHA_ASSISTIDA,
                RecursalFormalSectionLabels.DISTRIBUICAO_DEPENDENCIA_E_PROCESSO_REFERENCIA,
                RecursalFormalSectionLabels.SIGILO_PRIORIDADES_ALERTAS_DISTRIBUICAO
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("RESOLVER_COMPETENCIA_MULTIDIMENSIONAL", "resolver a competência recursal por critérios procedimental, material, pessoal, funcional, territorial e de alçada antes do sorteio governado do órgão julgador: "
                + RecursalWorkbenchSurfaceCatalog.distribuicaoWorkbench());
        checklist.put("TRATAR_AMBIGUIDADE_ASSISTIDA", "quando classe, assunto ou partes apontarem mais de um órgão possível, abrir escolha assistida da competência conflitante sem redistribuição cega, especialmente em cenários juizado x cível comum ou turma recursal x tribunal ordinário");
        checklist.put("PRESERVAR_DEPENDENCIA_E_REFERENCIA", "em incidente, recurso conexo ou ação originária derivada, manter processo de referência e vínculo de dependência sem perder o trilho recursal de origem");
        checklist.put("REGRAR_TURMA_RECURSAL_HIBRIDA", "se a turma recursal ainda não estiver implantada no mesmo sistema do processo de origem, orientar protocolo da ação originária recursal no sistema competente com indicação explícita do processo referência");
        checklist.put("APLICAR_SIGILO_E_PRIORIDADES", "transportar segredo de justiça, prioridade e demais alertas de distribuição para o órgão de destino sem alterar artificialmente prevenção, impedimento ou suspeição aparentes");
        checklist.put("VALIDAR_DADOS_MINIMOS_DISTRIBUICAO", "permitir distribuição apenas quando jurisdição, classe, assunto, partes e atributos de valor/sigilo forem suficientes para fixar a competência do destino");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("a distribuição recursal deve preservar sorteio governado do órgão julgador, salvo dependência legalmente justificada");
        alertas.add("ambiguidade de competência não pode ser resolvida por heurística opaca; o shell precisa expor a escolha assistida e a razão do conflito material");
        if (request.juizadoEspecial()) {
            alertas.add("como o caso vem de juizado especial, a rota precisa distinguir recurso inominado, agravo originário e eventual exceção quando a turma recursal ainda estiver em sistema diverso");
        }
        if (ramoAxis(request).equals("FAMILIA")) {
            alertas.add("no ramo de família, o protocolo deve reforçar sigilo mínimo desde a etapa de distribuição");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "resolver competência, ambiguidade, dependência e sistema de destino da subida recursal com critérios explícitos de distribuição, sem perder processo de referência, sigilo, prioridade e governança do sorteio.";
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }
}
