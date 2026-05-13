package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

public final class InstitutionalPanelProvisioningMessages {

    public static final String PANEL_COMPLETE = "painel_pronto_para_operacao_completa_no_pjb";
    public static final String SHARED_EXPERIENCE_COMPLETE = "painel_pronto_para_experiencia_compartilhada_no_pjb";
    public static final String FINDING_BLUEPRINT = "painel_sem_blueprint_canonico";
    public static final String FINDING_WORKSPACE = "painel_sem_workspace_processual";
    public static final String FINDING_ROUTE = "painel_sem_rota_inicial_resolvida";
    public static final String FINDING_SECTIONS = "painel_sem_secoes_primarias";
    public static final String FINDING_QUICK_ACTIONS = "painel_sem_acoes_rapidas";
    public static final String FINDING_GUARDS = "painel_sem_guardas_seguranca";
    public static final String FINDING_VISIBILITY = "painel_sem_regras_visibilidade";
    public static final String FINDING_TABS = "painel_sem_tabs_workspace";
    public static final String FINDING_WORKSPACE_ACTIONS = "painel_sem_acoes_workspace";
    public static final String FINDING_AUTHORITY_BANDS = "painel_sem_faixas_autoridade";
    public static final String FINDING_SEPARATORS = "painel_sem_separadores_processuais";
    public static final String FINDING_NOTIFICATIONS = "painel_sem_superficie_notificacoes";
    public static final String FINDING_CALENDAR = "painel_sem_superficie_calendario";
    public static final String FINDING_HEARINGS = "painel_sem_superficie_datas_audiencia";
    public static final String FINDING_READING = "painel_sem_superficie_modo_leitura";
    public static final String FINDING_TRIAGE = "painel_sem_superficie_triagem";
    public static final String FINDING_PRESENTATION = "painel_sem_superficie_apresentacao";
    public static final String FINDING_COLORS = "painel_sem_superficie_cores";
    public static final String FINDING_OPINION = "painel_sem_fluxo_parecer_minuta_manifestacao";
    public static final String FINDING_CALCULATOR = "painel_sem_superficie_calculadora_judicial";
    public static final String FINDING_HEARING_GOVERNANCE = "painel_sem_governanca_audiencia_compativel";
    public static final String FINDING_DESK_GOVERNANCE = "painel_sem_governanca_operacional_de_unidade_compativel";
    public static final String OPINION_READY = "painel_pronto_para_fluxo_parecer_manifestacao";
    public static final String CALCULATOR_READY = "painel_pronto_para_calculadora_judicial";
    public static final String HEARING_GOVERNANCE_READY = "painel_pronto_para_governanca_de_audiencias";
    public static final String DESK_GOVERNANCE_READY = "painel_pronto_para_governanca_operacional_de_unidade";

    private InstitutionalPanelProvisioningMessages() {
    }

    public static String panel(String value) {
        return "painel=" + value;
    }

    public static String profileKey(String value) {
        return "perfil_operacional=" + value;
    }

    public static String processProfile(String value) {
        return "process_profile=" + value;
    }

    public static String nominationRole(String value) {
        return "nomination_role=" + value;
    }

    public static String initialRoute(String value) {
        return "rota_inicial=" + value;
    }

    public static String catalogProfile(String value) {
        return "catalog_profile=" + value;
    }

    public static String sharedSurface(String key, String route) {
        return "shared_surface=" + key + "@" + route;
    }

    public static String readySharedSurface(String key) {
        return "shared_surface_ready=" + key;
    }
}
