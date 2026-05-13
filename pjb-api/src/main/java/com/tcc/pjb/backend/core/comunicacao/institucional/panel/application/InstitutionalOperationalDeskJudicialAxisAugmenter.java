package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import java.util.Objects;

final class InstitutionalOperationalDeskJudicialAxisAugmenter {

    private final InstitutionalOperationalDeskSupport support;

    InstitutionalOperationalDeskJudicialAxisAugmenter(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    void apply(InstitutionalOperationalDeskGovernanceDraft draft,
               InstitutionalOperationalDeskSnapshot snapshot) {
        String judicialAxis = snapshot.judicialAxis();
        String unitKind = snapshot.unitKind();
        InstitutionalProcessProfile processProfile = snapshot.processProfile();
        if (support.containsToken(judicialAxis, "CIVEL")) {
            draft.operationalDomains().add("CIVEL_CUMPRIMENTO_E_SANEAMENTO");
            draft.deskQueues().add("fila_saneamento_instrucao_civel=" + support.normalize(judicialAxis));
            draft.expeditionActs().add("expedir_citacao_intimacao_carta_precatoria_e_cumprimento_de_sentenca_civel");
            draft.conclusionActs().add("controlar_saneamento_instrucao_julgamento_e_fase_de_cumprimento_civel");
            draft.specializedFlows().add("civel_contestacao_saneamento_instrucao_julgamento_e_cumprimento");
        }
        if (support.containsToken(judicialAxis, "PENAL", "JURI", "CUSTODIA")) {
            draft.operationalDomains().add("PENAL_CUSTODIA_E_MANDADOS");
            draft.deskQueues().add("fila_penal_custodia_juri=" + support.normalize(judicialAxis));
            draft.expeditionActs().add("expedir_mandados_penais_termos_custodiais_e_comunicacoes_de_audiencia_penal");
            draft.conclusionActs().add("controlar_recebimento_denuncia_instrucao_decisao_pronuncia_ou_execucao_penal");
            draft.specializedFlows().add("penal_inquerito_acao_penal_audiencias_custodia_ou_juri");
            draft.forbiddenActs().add("fluxo_penal_nao_se_mistura_com_rito_civel_ou_secretaria_estranha");
        }
        if (support.containsToken(judicialAxis, "FAZENDA_PUBLICA", "EXECUCAO_FISCAL")) {
            draft.operationalDomains().add("FAZENDA_REQUISITORIOS_E_EXECUCAO_FISCAL");
            draft.deskQueues().add("fila_fazenda_execucao_fiscal_e_requisitorios=" + support.normalize(judicialAxis));
            draft.distributionActs().add("controlar_dependencia_redirecionamento_e_redistribuicao_da_fazenda_publica");
            draft.expeditionActs().add("operar_portal_fazendario_requisitorios_e_citacoes_da_execucao_fiscal");
            draft.conclusionActs().add("fechar_fluxo_de_calculo_baixa_suspensao_ou_extincao_na_fazenda_publica");
            draft.specializedFlows().add("fazenda_publica_execucao_fiscal_requisitorios_e_fluxo_fazendario");
        }
        if (support.containsToken(judicialAxis, "TRABALHO", "TRABALHISTA") || processProfile == InstitutionalProcessProfile.CONTADOR_JUDICIAL) {
            draft.operationalDomains().add("TRABALHISTA_AUDIENCIA_LIQUIDACAO_E_EXECUCAO");
            draft.deskQueues().add("fila_trabalhista_audiencia_liquidacao_execucao=" + support.normalize(judicialAxis));
            draft.expeditionActs().add("gerir_notificacao_audiencia_una_e_expedientes_da_execucao_trabalhista");
            draft.conclusionActs().add("controlar_liquidacao_calculos_homologacao_e_execucao_trabalhista");
            draft.specializedFlows().add("trabalhista_audiencia_una_liquidacao_calculo_e_execucao");
        }
        if (support.containsToken(judicialAxis, "ELEITORAL")) {
            draft.operationalDomains().add("ELEITORAL_CALENDARIO_E_URGENTISSIMOS");
            draft.deskQueues().add("fila_eleitoral_pauta_urgencias_e_calendario=" + support.normalize(judicialAxis));
            draft.expeditionActs().add("operar_intimacoes_requisicoes_e_cumprimentos_sensiveis_ao_calendario_eleitoral");
            draft.conclusionActs().add("priorizar_julgamentos_medidas_urgentes_e_publicacoes_eleitorais");
            draft.specializedFlows().add("eleitoral_pauta_urgencias_medidas_e_calendario_processual");
        }
        if (support.containsToken(judicialAxis, "MILITAR")) {
            draft.operationalDomains().add("MILITAR_DISCIPLINA_E_RITO_PROPRIO");
            draft.deskQueues().add("fila_militar_rito_proprio=" + support.normalize(judicialAxis));
            draft.expeditionActs().add("operar_comunicacoes_mandados_e_audiencias_do_rito_militar");
            draft.specializedFlows().add("militar_audiencias_comunicacoes_e_execucao_de_rito_proprio");
        }
        if (support.containsToken(unitKind, "SECRETARIA_SEGUNDO_GRAU")
                && !draft.specializedFlows().contains("segundo_grau_prevencao_pauta_sessao_acordao_e_publicacao")) {
            draft.specializedFlows().add("segundo_grau_prevencao_pauta_sessao_acordao_e_publicacao");
        }
    }
}
