package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalDeadlineNotificationBlueprint {

    private RecursalDeadlineNotificationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.PRAZO_RECURSAL_REAL);
        sections.add(RecursalFormalSectionLabels.CALENDARIO_RECURSAL_OPERACIONAL);
        sections.add(RecursalFormalSectionLabels.PAINEL_TEMPORAL_RECURSAL);
        sections.add(RecursalFormalSectionLabels.PREVIEW_NOTIFICACOES_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.PREFERENCIAS_NOTIFICACAO_RECURSAL);
        sections.add(RecursalFormalSectionLabels.INTIMACAO_MULTICANAL_RECURSAL);
        sections.add(RecursalFormalSectionLabels.AVISOS_JANELA_CONTRARRAZOES_ADESIVO);
        sections.add(RecursalFormalSectionLabels.AVISOS_PUBLICACAO_ACORDAO);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("REUSAR_PRAZO_REAL_EXISTENTE", "usar o prazo real já existente como motor-base do aviso recursal, sem deadline paralelo: "
                + String.join(" | ", rotasPrazoReal()));
        checklist.put("ABRIR_CALENDARIO_RECURSAL", "usar o calendário e o painel temporal existentes para abrir a janela do recurso, das contrarrazões, do adesivo, da pauta e da publicação: "
                + String.join(" | ", rotasCalendario()));
        checklist.put("GERAR_PREVIEW_DE_ALERTAS", "reaproveitar o preview de notificações do calendário para avisos recursais previsíveis, inclusive marcos críticos e janela de publicação: "
                + String.join(" | ", rotasPreview()));
        checklist.put("REUSAR_PREFERENCIAS_DE_NOTIFICACAO", "respeitar as preferências de notificação já cadastradas pelo usuário e não abrir configuração paralela só para recursal: "
                + String.join(" | ", rotasPreferencias()));
        checklist.put("ORQUESTRAR_INTIMACAO_MULTICANAL", "quando o caso exigir comunicação ativa, reaproveitar a trilha multicanal existente para processo e usuário já autenticado: "
                + String.join(" | ", rotasMulticanal()));
        checklist.put("PUBLICAR_AVISOS_POR_PERFIL", "os avisos devem aparecer em degraus coerentes para cidadão, representante técnico, secretaria e magistratura, sempre com base em "
                + filtroRecursal(recursoPrincipal, request));
        checklist.put("TRAVAR_EVENTOS_CRITICOS_RECURSAIS", "avisar pelo menos: prazo-base da interposição, risco de preparo, feriado local não comprovado, janela de contrarrazões, adesivo quando cabível, pauta, sustentação oral e publicação do acórdão");
        checklist.put("VEDAR_SCHEDULER_PARALELO", "não criar scheduler recursal fora da governança existente; integrar o recursal ao preview, ao calendário, às preferências e à entrega multicanal já presentes no projeto");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("avisos recursais devem reutilizar prazo real, calendário, preview de notificação e preferências existentes; não abrir motor paralelo de alerta");
        if (request.preparoInsuficiente()) {
            alertas.add("como há preparo insuficiente, o aviso deve destacar a janela legal de complementação antes de marcar deserção");
        } else if (!request.preparoEfetuado() && !request.autosEletronicos()) {
            alertas.add("como o preparo ainda não foi confirmado, o aviso deve destacar recolhimento e conferência antes do protocolo");
        }
        if (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado()) {
            alertas.add("há feriado local não comprovado; o aviso recursal deve subir a criticidade até a prova do feriado no ato da interposição");
        }
        if (request.recursoPrincipalInterposto()) {
            alertas.add("como já existe recurso principal interposto, a trilha deve avisar a janela de contrarrazões e eventual adesivo compatível");
        }
        if (classificacaoRecursal(recursoPrincipal).equals("EMBARGOS")) {
            alertas.add("nos embargos, avisar integração/correção do pronunciamento, publicação do julgamento integrativo e novo marco temporal quando houver alteração do resultado");
        } else {
            alertas.add("nos recursos, avisar subida, distribuição, relatoria, pauta, sustentação oral e publicação do acórdão conforme a rota do órgão competente");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "orquestrar avisos e notificações recursais reaproveitando prazo real, calendário operacional, preview de notificações, preferências e entrega multicanal já existentes, filtrando tudo por "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasPrazoReal() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.processoPrazoReal(),
                RecursalWorkbenchSurfaceCatalog.calendarPanel(),
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace()
        );
    }

    private static List<String> rotasCalendario() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace(),
                RecursalWorkbenchSurfaceCatalog.calendarPanel(),
                RecursalWorkbenchSurfaceCatalog.calendarInstitutionalBridge(),
                RecursalWorkbenchSurfaceCatalog.calendarInstitutionalFocus()
        );
    }

    private static List<String> rotasPreview() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace(),
                RecursalWorkbenchSurfaceCatalog.calendarPanel()
        );
    }

    private static List<String> rotasPreferencias() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.calendarPreferences(),
                RecursalWorkbenchSurfaceCatalog.notificationPreferencesUser(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingPixel()
        );
    }

    private static List<String> rotasMulticanal() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia(),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = blank(request.ramoProcessual()) ? "RAMO_NAO_MAPEADO" : request.ramoProcessual().trim().toUpperCase();
        String rito = request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO";
        return "ramo=" + ramo + ", rito=" + rito + ", classe=" + classificacaoRecursal(recursoPrincipal) + ", especie=" + recursoPrincipal;
    }

    private static String classificacaoRecursal(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
