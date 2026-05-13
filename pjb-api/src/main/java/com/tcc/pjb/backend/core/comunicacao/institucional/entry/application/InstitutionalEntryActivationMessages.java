package com.tcc.pjb.backend.core.comunicacao.institucional.entry.application;

public final class InstitutionalEntryActivationMessages {

    public static final String PROFILE_BOUND_TO_ENTRY = "perfil_operacional_ativo_amarrado_a_entrada_pos_login";
    public static final String ENTRY_DECIDED_BY_CONTEXT = "entrada_pos_login_decidida_por_contexto_funcional_e_confianca";
    public static final String ENTRY_REQUIRES_PJB_PROFILE = "entrada_institucional_depende_de_perfil_materializado_no_pjb";
    public static final String ENTRY_CANNOT_BYPASS_GOVERNANCE = "entrada_pos_login_nao_contorna_governanca_ou_step_up";
    public static final String PANEL_INSTITUTIONAL = "destino_painel_institucional";
    public static final String PANEL_PERSONAL = "destino_painel_pessoal";
    public static final String BLOCKED_CONTAINMENT = "destino_contencao_bloqueada";
    public static final String WAITING_STEP_UP = "destino_aguardando_step_up";
    public static final String WAITING_MANUAL_APPROVAL = "destino_aguardando_aprovacao_manual";
    public static final String WAITING_GOVBR_BINDING = "destino_aguardando_vinculo_govbr";
    public static final String WAITING_TRUSTED_DEVICE = "destino_aguardando_dispositivo_confiavel";
    public static final String WAITING_PANEL_PROVISIONING = "destino_aguardando_provisionamento_painel";
    public static final String PANEL_PROVISIONING_COMPLETE = "painel_institucional_completo_para_entrada";
    public static final String PANEL_SHARED_EXPERIENCE_READY = "painel_institucional_com_experiencia_compartilhada";

    private InstitutionalEntryActivationMessages() {
    }

    public static String targetEnvironment(String value) {
        return "target_environment=" + value;
    }

    public static String entryMode(String value) {
        return "entry_mode=" + value;
    }

    public static String profileState(String value) {
        return "profile_state=" + value;
    }

    public static String panelCode(String value) {
        return "panel_code=" + value;
    }

    public static String landingPath(String value) {
        return "landing_path=" + value;
    }

    public static String nomination(String value) {
        return "nomination_id=" + value;
    }

    public static String affiliation(String value) {
        return "affiliation_id=" + value;
    }

    public static String context(String value) {
        return "context_id=" + value;
    }

    public static String sensitiveAct(String value) {
        return "ato_sensivel_recomendado=" + value;
    }

    public static String panelProvisioning(boolean value) {
        return "panel_provisioning_complete=" + value;
    }

    public static String sharedExperience(boolean value) {
        return "panel_shared_experience_ready=" + value;
    }
}
