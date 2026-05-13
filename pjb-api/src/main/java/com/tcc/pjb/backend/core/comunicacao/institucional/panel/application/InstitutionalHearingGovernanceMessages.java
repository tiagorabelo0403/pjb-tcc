package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import java.util.Locale;

public final class InstitutionalHearingGovernanceMessages {

    public static final String PROFILE_MISSING = "perfil_operacional_inexistente";
    public static final String GOVERNANCE_MISSING = "governanca_audiencia=ausente";
    public static final String NO_SECTION = "painel_sem_secao_audiencia_habilitada";
    public static final String NO_REQUEST_TRACK = "painel_sem_pedido_audiencia_rastreavel";
    public static final String NO_OPERATIONAL_CAPACITY = "painel_sem_capacidade_operacional_de_pauta";
    public static final String DOCUMENTAL_SUPPORT_AUTONOMOUS_SCHEDULING = "agendamento_autonomo_por_apoio_documental";
    public static final String NO_AUTONOMOUS_JUDICIAL_DESIGNATION = "designacao_jurisdicional_autonoma_sem_chancela_do_magistrado";
    public static final String NO_FINAL_MINUTES_WITHOUT_PERMISSION = "registro_final_de_ata_sem_permissao_expressa";
    public static final String NO_SHARED_COLLECTIVE_ACCOUNT = "compartilhamento_de_conta_coletiva_para_pauta";
    public static final String NO_PRISON_FLOW_CHANGES_WITHOUT_ORDER = "alteracao_de_pauta_sem_ordem_judicial_ou_secretaria";
    public static final String NO_PUBLIC_MINOR_EXPOSURE = "exposicao_publica_de_menor_em_pauta_aberta";
    public static final String NO_CUSTODY_WITHOUT_PRESENTATION = "realizacao_de_custodia_sem_apresentacao_do_custodiado_ou_sem_registro_de_escolta";
    public static final String NO_LABOR_SPLIT_WITHOUT_CAUSE = "fragmentacao_indevida_de_audiencia_una_sem_justificativa";
    public static final String NO_ELECTORAL_SCHEDULING_OUTSIDE_CALENDAR = "pauta_eleitoral_sem_prioridade_calendario_oficial";
    public static final String NO_MILITARY_SCHEDULING_WITHOUT_CHAIN = "movimentacao_de_pauta_militar_sem_cadeia_de_comando_e_sigilo";
    public static final String NO_PUBLIC_TREASURY_AGREEMENT_WITHOUT_VALIDATION = "acordo_sem_validacao_do_ente_publico_em_audiencia";
    public static final String NO_RITE_CATALOG = "catalogo_audiencia_sem_ritos_relevantes";
    public static final String NO_OPERATIONAL_RITES = "perfil_operacional_sem_rito_com_agendamento_valido";
    public static final String NO_CROSS_UNIT_SCHEDULING = "mistura_de_unidade_ou_vara_na_pauta";
    public static final String NO_CROSS_BRANCH_SCHEDULING = "mistura_de_ramo_ou_competencia_na_pauta";
    public static final String NO_COMMUNICATION_WITHOUT_ORDER = "expedicao_de_comunicacoes_de_audiencia_sem_fluxo_valido";
    public static final String NO_BUNDLE_WITHOUT_SEGREGATION = "preparo_de_pasta_de_audiencia_sem_segregacao_funcional";
    public static final String HEARING_GOVERNANCE_ACTIVE = "governanca_audiencia=ativa";
    public static final String JUDICIAL_AUTHORIZATION_REQUIRED = "autorizacao_judicial_exigida_para_agendamento_operacional";
    public static final String PRISON_FLOW_PRESENTATION_CONFIRMATION = "fluxo_prisional_exige_confirmacao_de_apresentacao_para_audiencia";
    public static final String TRACKING_ONLY_PANEL = "painel_de_audiencia_com_acompanhamento_sem_agendamento_operacional";
    public static final String JUDICIAL_DESIGNATION_PRESERVED = "designacao_privativa_do_magistrado_preservada";
    public static final String SECRETARIAT_COORDINATION_REQUIRED = "coordenacao_secretaria_central_pauta_requerida";
    public static final String LEGAL_INSTITUTION_FOUNDATION = "partes_e_funcoes_essenciais_tem_espaco_para_pedir_audiencia_e_acompanhar_pauta_no_pjb";
    public static final String OPERATIONAL_CHAIN_FOUNDATION = "operacao_de_pauta_reserva_sala_link_ata_e_presenca_fica_no_pjb_com_trilha_de_autoria";
    public static final String PRISON_FLOW_FOUNDATION = "fluxo_prisional_mantem_confirmacao_de_apresentacao_e_audiencia_de_custodia_no_mesmo_contexto";
    public static final String HEARING_COMMUNICATION_FOUNDATION = "expedicao_de_intimacoes_e_comunicacoes_de_audiencia_fica_segregada_na_unidade_e_na_caixa_correta";
    public static final String HEARING_BUNDLE_FOUNDATION = "preparo_documental_da_audiencia_fica_separado_da_designacao_jurisdicional_e_da_ata_final";
    public static final String UNIT_ISOLATION_FOUNDATION = "isolamento_por_unidade_vara_caixa_e_competencia_evitar_mistura_de_pautas";
    public static final String JUDICIAL_OVERSIGHT_FOUNDATION = "magistrado_e_gabinete_mantem_supervisao_integral_sobre_pauta_intimacao_e_ata";

    private InstitutionalHearingGovernanceMessages() {
    }

    public static String sectionVisible(boolean value) {
        return "secao_audiencias=" + value;
    }

    public static String hearingProfile(String value) {
        return "perfil_audiencia=" + normalize(value);
    }

    public static String nominationRole(String value) {
        return "papel_nomeacao=" + normalize(value);
    }

    public static String scope(String value) {
        return "escopo=" + normalize(value);
    }

    public static String workspaceTabs(int value) {
        return "workspace_tabs=" + value;
    }

    public static String workspaceActions(int value) {
        return "workspace_actions=" + value;
    }

    public static String totalRites(int value) {
        return "total_ritos_audiencia=" + value;
    }

    public static String operationalRites(int value) {
        return "total_ritos_operacionais_audiencia=" + value;
    }

    public static String trackingOnlyRites(int value) {
        return "total_ritos_apenas_acompanhamento=" + value;
    }

    public static String operationalPanel() {
        return "painel_com_poder_operacional_de_pauta";
    }

    public static String riteFoundation(String riteCode, String branch, String hearingKind) {
        return "rito=" + normalize(riteCode) + ";ramo=" + normalize(branch) + ";audiencia=" + normalize(hearingKind);
    }

    public static String jurisdictionAxis(String value) {
        return "eixo_jurisdicao=" + normalize(value);
    }

    public static String specializationAxis(String value) {
        return "eixo_especializacao=" + normalize(value);
    }

    public static String queueScopeKey(String value) {
        return "queue_scope=" + normalize(value);
    }

    public static String schedulingScopeKey(String value) {
        return "scheduling_scope=" + normalize(value);
    }

    public static String unitIsolation(boolean value) {
        return "isolamento_unidade=" + value;
    }

    public static String communications(boolean value) {
        return "comunicacoes_audiencia=" + value;
    }

    public static String hearingBundle(boolean value) {
        return "preparo_pasta_audiencia=" + value;
    }

    public static String oversightActors(int value) {
        return "atores_supervisao=" + value;
    }

    public static String operationalQueues(int value) {
        return "filas_operacionais_audiencia=" + value;
    }

    public static String segregationGuards(int value) {
        return "guardas_segregacao=" + value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }
}
