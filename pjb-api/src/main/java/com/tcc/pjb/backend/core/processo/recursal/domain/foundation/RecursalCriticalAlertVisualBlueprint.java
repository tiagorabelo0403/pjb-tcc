package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalCriticalAlertVisualBlueprint {

    private RecursalCriticalAlertVisualBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.ALERTA_VERMELHO_PRAZO_CRITICO);
        sections.add(RecursalFormalSectionLabels.ALERTA_MULTICANAL_TODOS_CANAIS);
        sections.add(RecursalFormalSectionLabels.CORES_PROCESSUAIS_CRITICIDADE_RECURSAL);
        sections.add(RecursalFormalSectionLabels.VOTOS_VIVOS_RECURSAL);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("PINTAR_VERMELHO_SEM_ATO_CRITICO", "quando o prazo estiver próximo e o polo técnico ainda não tiver praticado o ato esperado, o processo deve ficar vermelho nos painéis já existentes, sem nova paleta e sem dashboard satélite: "
                + String.join(" | ", rotasCromaticas()));
        checklist.put("ENTREGAR_ALERTA_MULTICANAL", "o mesmo evento crítico deve acionar o preview, a preferência e a entrega multicanal já existentes, incluindo PJB, e-mail e número cadastrado na mesma espinha de notificações: "
                + String.join(" | ", rotasMulticanal()));
        checklist.put("FILTRAR_AUDIENCIA_DO_ALERTA", "o alerta vermelho deve respeitar a audiência de cada degrau: cidadão só em processo próprio; representação técnica por ramo/rito/classe/espécie; secretaria e magistratura com criticidade operacional");
        checklist.put("REUSAR_VOTOS_VIVOS", "quando houver julgamento colegiado ou plenário em recurso/embargos, reaproveitar os votos vivos e o stream já existente para espelhar composição, proclamação e andamento em tempo real: "
                + String.join(" | ", rotasVotosVivos()));
        checklist.put("VEDAR_ALERTA_FANTASMA", "após protocolo, saneamento, publicação útil ou superação do risco, o vermelho deve regredir para a cor processual já prevista na legenda oficial, evitando ruído permanente");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("prazo próximo sem ato do legitimado técnico deve acender vermelho no card, no radar e na fila do processo, reaproveitando a cor e a legenda já existentes");
        alertas.add("o mesmo risco crítico precisa ser entregue pelos canais já existentes do PJB, incluindo preview interno, e-mail e número cadastrado, sem abrir motor paralelo de envio");
        if (request.desejaSustentacaoOral() || recursoPrincipal.equals("APELACAO") || recursoPrincipal.equals("RECURSO_INOMINADO") || recursoPrincipal.equals("EMBARGOS_DIVERGENCIA")) {
            alertas.add("se o caso estiver em julgamento colegiado, os votos vivos devem espelhar o andamento em tempo real para representação técnica, operação e cidadão autenticado no limite do sigilo");
        }
        if (request.preparoInsuficiente() || (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado())) {
            alertas.add("preparo insuficiente ou feriado local não comprovado elevam imediatamente o vermelho para a maior criticidade operacional");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "ativar alerta vermelho de prazo crítico, entrega multicanal e integração de votos vivos no recurso/embargos, reaproveitando cores, legendas, notificações e streams já existentes sob "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasCromaticas() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.uiLegend(),
                RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual(),
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel()
        );
    }

    private static List<String> rotasMulticanal() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.notificationPreferencesUser(),
                RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia()
        );
    }

    private static List<String> rotasVotosVivos() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenJulgamentoVotesStream(),
                RecursalWorkbenchSurfaceCatalog.julgamentoVotesStream(),
                RecursalWorkbenchSurfaceCatalog.citizenJulgamentos(),
                RecursalWorkbenchSurfaceCatalog.julgamentoProcessos(),
                RecursalWorkbenchSurfaceCatalog.malhaColegiada(0L)
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
