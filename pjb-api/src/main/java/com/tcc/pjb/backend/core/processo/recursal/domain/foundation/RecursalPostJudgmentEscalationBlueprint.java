package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalPostJudgmentEscalationBlueprint {

    private RecursalPostJudgmentEscalationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.POS_JULGAMENTO_RECURSAL_ESCALONADO);
        sections.add(RecursalFormalSectionLabels.MUDANCA_DEGRAU_PUBLICACAO_INTIMACAO);
        sections.add(RecursalFormalSectionLabels.PUBLICACAO_REABERTURA_PRAZO);
        sections.add(RecursalFormalSectionLabels.TRANSITO_OU_NOVA_SUBIDA);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("SINCRONIZAR_PUBLICACAO_E_INTIMACAO", "depois do julgamento, sincronizar publicação, intimação e mudança de degrau da escada recursal sem criar malha paralela: "
                + String.join(" | ", rotasPosJulgamento()));
        checklist.put("REABRIR_JANELA_RECURSAL_SUBSEQUENTE", "quando houver publicação de acórdão ou decisão recursal, recalcular a janela subsequente e republicar o prazo real, o calendário e o preview multicanal: "
                + String.join(" | ", rotasPrazoCalendario()));
        checklist.put("ATUALIZAR_PARTES_REPRESENTANTES_E_OPERACAO", "autor, réu, representantes técnicos, secretaria e magistratura devem enxergar a mudança de degrau com a criticidade compatível e sem perda do vínculo com o processo de origem");
        checklist.put("DEFINIR_TRANSITO_OU_NOVA_SUBIDA", "após a publicação, decidir entre encerramento da trilha, trânsito, embargos no mesmo órgão, novo recurso interno, recurso excepcional ou agravo excepcional, reaproveitando a mesma escada operacional");
        checklist.put("PRESERVAR_CIDADAO_PROCESSO_PROPRIO", "no pós-julgamento, o cidadão continua vendo apenas processo próprio, com linguagem externa, última movimentação, cor processual e próximo marco recursal permitido");
        checklist.put("VEDAR_DUPLICACAO_POS_JULGAMENTO", "não criar trilha paralela de pós-julgamento; a atualização deve entrar nos painéis, workspaces, prazos, calendário e notificações já existentes");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o pós-julgamento deve mudar o degrau visível do processo assim que houver pauta encerrada, resultado proclamado e publicação útil do acórdão ou da decisão");
        if (recursoPrincipal.startsWith("EMBARGOS")) {
            alertas.add("quando a rota for de embargos, o pós-julgamento deve manter o vínculo com o mesmo órgão prolator e só abrir nova subida se houver via subsequente realmente cabível");
        } else {
            alertas.add("quando a rota for recurso, a publicação deve abrir imediatamente a análise de nova subida, contrarrazões subsequentes, trânsito ou estabilização da decisão");
        }
        if (request.desejaSustentacaoOral()) {
            alertas.add("julgamento com sustentação oral exige reforço na virada de degrau após a sessão, inclusive publicação, ciência e atualização dos painéis do time recursal");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar o pós-julgamento recursal com mudança de degrau, publicação, eventual reabertura de prazo, trânsito ou nova subida, reaproveitando julgamentos, prazo real, calendário e notificações já existentes, sob "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasPosJulgamento() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenJulgamentos(),
                RecursalWorkbenchSurfaceCatalog.citizenJulgamentoDetail(),
                RecursalWorkbenchSurfaceCatalog.julgamentoProcessos(),
                RecursalWorkbenchSurfaceCatalog.julgamentoDetail(),
                RecursalWorkbenchSurfaceCatalog.publicProcessTimeline()
        );
    }

    private static List<String> rotasPrazoCalendario() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.processoPrazoReal(),
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace(),
                RecursalWorkbenchSurfaceCatalog.calendarPanel(),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = request.ramoProcessual() == null || request.ramoProcessual().isBlank()
                ? "RAMO_NAO_MAPEADO"
                : request.ramoProcessual().trim().toUpperCase();
        String rito = request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO";
        return "ramo=" + ramo + ", rito=" + rito + ", classe=" + (recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO") + ", especie=" + recursoPrincipal;
    }
}
