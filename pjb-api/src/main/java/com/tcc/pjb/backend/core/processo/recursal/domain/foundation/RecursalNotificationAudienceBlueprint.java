package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalNotificationAudienceBlueprint {

    private RecursalNotificationAudienceBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.DEGRAUS_NOTIFICACAO_POR_PERFIL);
        sections.add(RecursalFormalSectionLabels.CRITICIDADE_PRAZO_RECURSAL);
        sections.add(RecursalFormalSectionLabels.AVISOS_CIDADAO_PROCESSO_PROPRIO);
        sections.add(RecursalFormalSectionLabels.AVISOS_REPRESENTACAO_TECNICA);
        sections.add(RecursalFormalSectionLabels.AVISOS_SECRETARIA_MAGISTRATURA);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ESCALONAR_ALERTA_CIDADAO", "o cidadão só deve receber aviso de processo próprio, com linguagem externa e sem abrir detalhes táticos além do necessário: "
                + String.join(" | ", rotasCidadao()));
        checklist.put("ESCALONAR_ALERTA_REPRESENTACAO", "advocacia, Defensoria, Procuradoria e Ministério Público devem receber avisos táticos da janela recursal, já filtrados por ramo, rito, classe e espécie: "
                + String.join(" | ", rotasRepresentacao()));
        checklist.put("ESCALONAR_ALERTA_SECRETARIA_MAGISTRATURA", "secretaria, distribuição, gabinete e colegiado devem receber a criticidade operacional do prazo, da pauta e da publicação sem fila paralela: "
                + String.join(" | ", rotasSecretariaMagistratura()));
        checklist.put("TRAVAR_CRITICIDADE_POR_RAMO", "graduar a criticidade do aviso pela combinação " + filtroRecursal(recursoPrincipal, request)
                + ", reforçando ramos de maior sensibilidade e não tratando todos os recursos como equivalentes");
        checklist.put("REUTILIZAR_PREFERENCIAS_E_ENTREGA", "respeitar preferências já cadastradas, preview já existente e entrega multicanal já disponível; o recursal só deve especializar a audiência e a criticidade");
        checklist.put("VEDAR_DUPLICACAO_DE_CENTRAL", "não criar central recursal paralela de notificações; o escalonamento deve entrar nos painéis e workspaces já existentes do cidadão, da representação e da operação");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o cidadão recebe aviso externo apenas para processo próprio, com ênfase em última movimentação, mudança de degrau e data-limite visível");
        alertas.add("a representação técnica recebe aviso tático completo de interposição, contrarrazões, adesivo, preparo, pauta, sustentação oral e publicação");
        alertas.add("secretaria e magistratura recebem aviso operacional de criticidade, inclusive risco de perda de janela, pauta próxima e publicação com reabertura de prazo");
        if (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado()) {
            alertas.add("sem comprovação do feriado local, a criticidade deve subir imediatamente para representação técnica e operação interna antes do protocolo");
        }
        if (request.preparoInsuficiente()) {
            alertas.add("preparo insuficiente exige escalonamento alto para a representação técnica e para a secretaria, evitando deserção por perda da janela de complementação");
        }
        String ramo = ramo(request);
        if (ramo.equals("PENAL") || ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            alertas.add("o ramo " + ramo + " exige criticidade reforçada e exposição externa mais comedida por sensibilidade e sigilo graduado");
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("no juizado, os avisos devem apontar a turma recursal e a janela própria do recurso inominado, sem linguagem de apelação clássica");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "escalonar os alertas recursais por perfil e criticidade, reaproveitando a central já existente de prazo, calendário, preview, preferências e entrega multicanal, com filtros por "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasCidadao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenOwnProcesses(),
                RecursalWorkbenchSurfaceCatalog.citizenProcessOverview(),
                RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual(),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview()
        );
    }

    private static List<String> rotasRepresentacao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch()
        );
    }

    private static List<String> rotasSecretariaMagistratura() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.calendarPanel(),
                RecursalWorkbenchSurfaceCatalog.processualPendenciasPainel(),
                RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchQuickActions(),
                RecursalWorkbenchSurfaceCatalog.magistratureExecutiveDashboard(),
                RecursalWorkbenchSurfaceCatalog.magistraturaAutomationPreview()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        return "ramo=" + ramo(request)
                + ", rito=" + (request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO")
                + ", classe=" + classificacao(recursoPrincipal)
                + ", especie=" + recursoPrincipal
                + ", criticidade=" + criticidadeBase(request);
    }

    private static String classificacao(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static String criticidadeBase(RecursalAutomationRequest request) {
        if (request.preparoInsuficiente() || (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado())) {
            return "ALTA";
        }
        String ramo = ramo(request);
        if (ramo.equals("PENAL") || ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            return "ALTA";
        }
        if (request.desejaSustentacaoOral()) {
            return "MEDIA_ALTA";
        }
        return "PADRAO";
    }

    private static String ramo(RecursalAutomationRequest request) {
        return request.ramoProcessual() == null || request.ramoProcessual().isBlank()
                ? "GERAL"
                : request.ramoProcessual().trim().toUpperCase();
    }
}
