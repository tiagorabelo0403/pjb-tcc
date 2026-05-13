package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import java.util.Locale;

public final class InstitutionalOperationalDeskGovernanceMessages {

    public static final String GOVERNANCE_ACTIVE = "governanca_mesa_operacional=ativa";
    public static final String MISSING_PROFILE = "mesa_operacional_sem_perfil_materializado";
    public static final String MISSING_UNIT_SCOPE = "mesa_operacional_sem_amarra_tribunal_comarca_unidade";
    public static final String CROSS_VARA_BLOCK = "mistura_operacional_entre_varas_ou_especializacoes";
    public static final String JUDGE_OVERRIDE_FOUNDATION = "magistrado_mantem_correcao_e_supervisao_integral_sobre_fluxos_criticos";
    public static final String SECRETARIAT_FOUNDATION = "secretaria_prepara_certifica_intima_e_entrega_o_processo_redondo_sem_invadir_decisao";
    public static final String ASSESSOR_FOUNDATION = "assessoria_prepara_minutas_dossies_pareceres_e_apoio_tecnico_em_fila_propria";
    public static final String TRIAGE_FOUNDATION = "triagem_classifica_prioriza_e_encaminha_sem_romper_autoria_do_ato_final";
    public static final String MANDATE_FOUNDATION = "mandados_certidoes_e_expedientes_ficam_em_fluxo_segregado_da_unidade";
    public static final String COMMUNICATION_FOUNDATION = "comunicacoes_processuais_respeitam_secretaria_vara_caixa_e_competencia";
    public static final String BATCH_FOUNDATION = "operacoes_em_lote_dependem_de_perfil_forte_e_trilha_auditavel";
    public static final String DISTRIBUTION_FOUNDATION = "protocolo_distribuicao_autuacao_e_entrada_sao_controlados_por_filas_e_amarra_operacional";
    public static final String EXPEDITION_FOUNDATION = "expedientes_mandados_oficios_editais_e_cartas_permanecem_em_fluxo_segregado_e_auditavel";
    public static final String CONCLUSION_FOUNDATION = "conclusoes_gabinete_minutas_e_retorno_ao_magistrado_respeitam_fronteiras_da_unidade";
    public static final String QUEUE_MANAGEMENT_FOUNDATION = "gestao_de_filas_coberturas_e_substituicoes_exige_amarra_forte_por_unidade_vara_e_caixa";
    public static final String DISTRIBUTION_PORTAL_FOUNDATION = "porta_de_entrada_autuacao_prevencao_e_redistribuicao_exigem_trilha_forte_sem_salto_de_orgao";
    public static final String GABINETE_FOUNDATION = "gabinete_conclusoes_minutas_e_supervisao_judicial_ficam_em_bloco_reservado_da_unidade";
    public static final String UPJ_FOUNDATION = "upj_compartilha_serventia_mas_preserva_vara_origem_fronteira_e_autoria_do_fluxo";
    public static final String JUIZADO_FOUNDATION = "juizado_operacionaliza_sessao_termo_e_celeridade_sem_importar_ritual_incompativel";
    public static final String SECOND_DEGREE_FOUNDATION = "segundo_grau_controla_pauta_colegiada_prevencao_recursal_acordao_e_publicacao_em_trilha_propria";
    public static final String COUNTING_FOUNDATION = "contadoria_trabalha_com_memoria_versoes_e_retorno_tecnico_sem_substituir_decisao";
    public static final String MANDATE_CENTER_FOUNDATION = "central_de_mandados_segmenta_roteiro_grupo_resultado_e_devolucao_com_trilha_forte";
    public static final String CEJUSC_FOUNDATION = "cejusc_gerencia_pre_sessao_termos_retorno_ao_juizo_e_disponibilidade_sem_substituir_o_feito";
    public static final String COORDINATION_FOUNDATION = "coordenacao_da_unidade_governa_capacidade_filas_e_bloco_funcional_sem_romper_autoria_do_ato";
    public static final String SPECIALIZED_FLOW_FOUNDATION = "catalogo_de_fluxos_especializados_materializado_por_eixo_unidade_e_rito_operacional";

    private InstitutionalOperationalDeskGovernanceMessages() {
    }

    public static String organizationScope(String value) {
        return "escopo_organizacional=" + normalize(value);
    }

    public static String territorialScope(String value) {
        return "escopo_territorial=" + normalize(value);
    }

    public static String groupingKey(String value) {
        return "agrupamento_unidade=" + normalize(value);
    }

    public static String isolationMode(String value) {
        return "modo_isolamento=" + normalize(value);
    }

    public static String operationalDomains(int value) {
        return "dominios_operacionais=" + value;
    }

    public static String topology(int value) {
        return "topologia_unidade=" + value;
    }


    public static String judicialAxis(String value) {
        return "eixo_judicial=" + normalize(value);
    }

    public static String unitKind(String value) {
        return "tipo_unidade=" + normalize(value);
    }

    public static String assignmentBoundaryKey(String value) {
        return "fronteira_atribuicao=" + normalize(value);
    }

    public static String queues(int value) {
        return "filas_mesa_operacional=" + value;
    }

    public static String boundaries(int value) {
        return "fronteiras_mesa_operacional=" + value;
    }

    public static String counterparts(int value) {
        return "contrapartes_mesa_operacional=" + value;
    }

    public static String specializedFlows(int value) {
        return "fluxos_especializados=" + value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }
}
