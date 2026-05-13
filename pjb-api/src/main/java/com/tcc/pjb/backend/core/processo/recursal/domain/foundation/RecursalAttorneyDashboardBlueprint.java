package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalAttorneyDashboardBlueprint {

    private RecursalAttorneyDashboardBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.PAINEL_ADVOGADO_CITACOES_INTIMACOES,
                RecursalFormalSectionLabels.PAINEL_ADVOGADO_AUDIENCIAS_RECURSOS_SESSOES,
                RecursalFormalSectionLabels.PAINEL_ADVOGADO_PENDENCIAS_RELACAO_PROCESSOS,
                RecursalFormalSectionLabels.PAINEL_ADVOGADO_ATALHOS_CONFIGURACOES
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_LOCALIZADORES_INTIMACAO_E_PRAZO", "exibir no painel do advogado localizadores separados para citações/intimações pendentes, prazo em aberto, urgências e homologação de acordo recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoPainelPrincipal(),
                RecursalWorkbenchSurfaceCatalog.advogadoCitacoesIntimacoes(),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia())));
        checklist.put("SINCRONIZAR_AUDIENCIAS_RECURSOS_E_SESSOES", "unificar audiências futuras, recursos do tribunal e sessões de julgamento no mesmo cockpit profissional recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAudienciasFuturas(),
                RecursalWorkbenchSurfaceCatalog.advogadoRecursosTribunal(),
                RecursalWorkbenchSurfaceCatalog.advogadoSessoesJulgamento(),
                RecursalWorkbenchSurfaceCatalog.processualPautaAudiencia())));
        checklist.put("PRESERVAR_AREA_TRABALHO_E_RELACAO_PROCESSOS", "manter pendências, relação de processos, últimas movimentações e filtros do advogado no workspace profissional já existente: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoAreaTrabalho(),
                RecursalWorkbenchSurfaceCatalog.advogadoRelacaoProcessos(),
                RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard())));
        checklist.put("FIXAR_ATALHOS_E_PAINEL_DETALHADO", "permitir alternância limpa entre painel principal e painel detalhado, com atalhos configuráveis e sem abrir shell paralelo para o recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.advogadoPainelPrincipal(),
                RecursalWorkbenchSurfaceCatalog.advogadoPainelDetalhado(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard())));
        checklist.put("TRAVAR_LENTE_A_ENVOLVIDOS", "o painel do advogado só aprofunda a lente recursal quando houver vínculo real com os autos; consulta pública e busca genérica continuam neutras");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o painel do advogado não pode colapsar citações, intimações, audiências, recursos e sessões em uma única fila genérica sem estado operacional distinto");
        alertas.add("o detalhamento recursal precisa preservar o mesmo workspace profissional, evitando cockpit paralelo fora do eixo de advogado/escritório já existente");
        if (!request.usuarioEnvolvidoNosAutos() || request.consultaProcessualGenerica()) {
            alertas.add("sem vínculo processual autenticado, o painel do advogado deve permanecer neutro e sem aprofundamento recursal contextual");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "publicar um painel recursal completo do advogado com citações, intimações, audiências, recursos, sessões, área de trabalho, relação de processos e atalhos configuráveis dentro do workspace profissional já existente.";
    }
}
