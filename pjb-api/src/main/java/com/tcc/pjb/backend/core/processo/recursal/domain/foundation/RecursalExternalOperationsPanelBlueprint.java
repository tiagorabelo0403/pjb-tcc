package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalExternalOperationsPanelBlueprint {

    private RecursalExternalOperationsPanelBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.PAINEL_EXTERNO_EXPEDIENTES_ACERVO_AGRUPADORES,
                RecursalFormalSectionLabels.PAINEL_EXTERNO_CITACOES_INTIMACOES_AUDIENCIAS,
                RecursalFormalSectionLabels.PAINEL_EXTERNO_PENDENCIAS_SUBSTABELECIMENTO,
                RecursalFormalSectionLabels.ESCRITORIO_ADVOCACIA_E_ASSISTENTES
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ABRIR_EXPEDIENTES_E_ACERVO_FILTRADOS", "no painel externo, exibir expedientes, acervo, agrupadores, relação de processos e últimas movimentações já filtradas por rito, classe, espécie e vínculo real com os autos: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceMainDashboard(),
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.publicConsultaWorkspace())));
        checklist.put("ORQUESTRAR_INTIMACOES_E_AUDIENCIAS", "abrir citações/intimações pendentes, prazos abertos, intimação de pauta e pauta de audiência em uma visão recursal coerente com calendário, notificação e ciência: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace(),
                RecursalWorkbenchSurfaceCatalog.calendarPanel(),
                RecursalWorkbenchSurfaceCatalog.processualPautaAudiencia(),
                RecursalWorkbenchSurfaceCatalog.notificationPreferencesUser())));
        checklist.put("PRESERVAR_PENDENCIAS_E_SUBSTABELECIMENTO", "manter petições salvas para distribuição futura, movimentações preparadas, substabelecimentos recebidos/enviados e pendências do advogado dentro da área de trabalho já existente: "
                + RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel());
        checklist.put("REUSAR_ESCRITORIO_E_ASSISTENTES", "reaproveitar cadastro de escritório, assistentes, acesso do afiliado e modo leitura do escritório sem cockpit paralelo para o recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.officeProcessAccess(),
                RecursalWorkbenchSurfaceCatalog.officeProcessReadingMode(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceOrganizationalDashboard())));
        checklist.put("TRAVAR_COMUTACAO_A_ENVOLVIDOS", "o painel externo só comuta para o contexto recursal quando o usuário estiver envolvido nos autos; busca genérica e consulta pública neutra continuam sem cockpit profissional adicional");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o painel externo recursal precisa distinguir expedientes pendentes, prazo em aberto, intimação de pauta, audiências e pendências preparadas sem misturar tudo em uma única caixa genérica");
        alertas.add("escritório, assistente e substabelecimento devem operar sobre as mesmas superfícies de trabalho, sem abrir modelo recursal satélite fora do workspace profissional existente");
        if (!request.usuarioEnvolvidoNosAutos() || request.consultaProcessualGenerica()) {
            alertas.add("como não há vínculo recursal autenticado forte, a lente externa deve permanecer neutra e sem comutação profissional aprofundada");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "organizar o painel externo recursal com expedientes, acervo, intimações, audiências, pendências, escritório, assistentes e substabelecimentos no mesmo workspace já existente, filtrando tudo pelo contexto ativo do recurso.";
    }
}
