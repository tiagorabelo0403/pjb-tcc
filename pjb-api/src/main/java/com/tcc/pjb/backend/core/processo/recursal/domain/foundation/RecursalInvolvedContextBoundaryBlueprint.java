package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalInvolvedContextBoundaryBlueprint {

    private RecursalInvolvedContextBoundaryBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.APLICACAO_CONTEXTO_RECURSAL_EMBARGOS,
                RecursalFormalSectionLabels.FRONTEIRA_ENVOLVIMENTO_PROCESSO_ATIVO,
                RecursalFormalSectionLabels.BUSCA_PROCESSUAL_NEUTRA_SEM_COMUTACAO,
                RecursalFormalSectionLabels.SUPERFICIE_PUBLICA_EXISTENTE_PRESERVADA
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("APLICAR_NO_PAINEL_RECURSAL", "aplicar a comutação contextual também nos painéis próprios de recurso e embargos, porque a abertura do processo ativo nesses painéis precisa herdar rito, tribunal, prazo, linguagem e filtros do caso concreto");
        checklist.put("TRAVAR_ENVOLVIMENTO_REAL", "só acionar shell contextual quando houver vínculo real com os autos; perfis envolvidos devem abrir contexto dedicado, enquanto consulta genérica não deve virar cockpit de rito");
        checklist.put("PRESERVAR_BUSCA_NEUTRA", "na busca de processos sem envolvimento, manter a superfície já existente do projeto sem trocar cards, atalhos ou linguagem contextual: " + String.join(" | ", neutralSearchSurfaces()));
        checklist.put("MANTER_CAMADA_PUBLICA_EXISTENTE", "quando o usuário estiver apenas pesquisando ou visualizando processo alheio, preservar a leitura pública existente do PJB e bloquear comutação para painéis privados/operacionais");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "a mudança de comportamento por rito precisa existir nos painéis recursais e de embargos do processo ativo, não só na lente de 1º grau",
                "buscar processo sem envolvimento não pode transformar o PJB em cockpit privado do rito; nesse caso a experiência deve continuar neutra e pública",
                "a fronteira correta é vínculo real com os autos: envolvido comuta, pesquisador externo só visualiza o que já existe no projeto"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "aplicar a comutação contextual por rito e tribunal também nos painéis de recurso e embargos do processo ativo, mas preservar a busca processual genérica em modo neutro quando não houver envolvimento real nos autos.";
    }

    public static boolean deveComutarShell(RecursalAutomationRequest request) {
        return request.usuarioEnvolvidoNosAutos() && !request.consultaProcessualGenerica();
    }

    private static List<String> neutralSearchSurfaces() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.publicConsultaWorkspace(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaProcesso(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaPageResolve()
        );
    }
}
