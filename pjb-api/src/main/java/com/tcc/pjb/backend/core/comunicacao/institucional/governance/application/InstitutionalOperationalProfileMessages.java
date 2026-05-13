package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

public final class InstitutionalOperationalProfileMessages {

    public static final String PROFILE_VISIBLE_IN_PJB = "perfil_materializado_e_visivel_no_pjb";
    public static final String PROFILE_DERIVED_FROM_NOMINATION = "perfil_operacional_derivado_de_nomeacao_institucional";
    public static final String PROFILE_ROUTED_BY_PANEL = "perfil_operacional_direcionado_por_painel_e_contexto";
    public static final String PROFILE_CANNOT_BYPASS_GOVERNANCE = "perfil_operacional_nao_contorna_governanca_de_confianca";

    private InstitutionalOperationalProfileMessages() {
    }

    public static String state(String state) {
        return "profile_state=" + state;
    }

    public static String audience(String audience) {
        return "profile_audience=" + audience;
    }

    public static String unit(String unitCode) {
        return "unit_code=" + unitCode;
    }

    public static String box(String caixaCodigo) {
        return "box_code=" + caixaCodigo;
    }

    public static String role(String role) {
        return "nomination_role=" + role;
    }
}
