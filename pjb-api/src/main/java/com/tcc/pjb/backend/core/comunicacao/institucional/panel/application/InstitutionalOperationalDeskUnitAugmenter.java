package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import java.util.Objects;

final class InstitutionalOperationalDeskUnitAugmenter {

    private final InstitutionalOperationalDeskSupport support;

    InstitutionalOperationalDeskUnitAugmenter(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    void apply(InstitutionalOperationalDeskGovernanceDraft draft,
               InstitutionalOperationalDeskSnapshot snapshot) {
        String unitKind = snapshot.unitKind();
        String judicialAxis = snapshot.judicialAxis();
        InstitutionalOperationalDeskUnitFingerprint fingerprint = snapshot.fingerprint();
        InstitutionalProcessProfile processProfile = snapshot.processProfile();
        if (support.containsToken(unitKind, "PROTOCOLO_DISTRIBUICAO")) {
            draft.unitTopology().add("bloco_funcional=PROTOCOLO_DISTRIBUICAO_AUTUACAO");
            draft.operationalDomains().add("PORTA_DE_ENTRADA_E_PREVENCAO");
            draft.deskQueues().add("fila_protocolo_autuacao=" + support.normalize(fingerprint.groupingKey()));
            draft.deskQueues().add("fila_prevencao_dependencia=" + support.normalize(fingerprint.specializationCluster()));
            draft.deskQueues().add("fila_redistribuicao_formal=" + support.normalize(fingerprint.varaCluster()));
            draft.assignmentBoundaries().add("protocolo_distribuicao_nao_assume_cumprimento_da_vara_sem_encaminhamento_formal");
            draft.assignmentBoundaries().add("porta_de_entrada_preserva_prevencao_dependencia_e_orgao_destino");
            draft.counterpartScopes().add("NUCLEO_DE_DISTRIBUICAO_PROTOCOLO_E_MALOTE");
            draft.secretariatActs().add("validar_prevencao_dependencia_e_autuacao_antes_da_distribuicao");
            draft.secretariatActs().add("controlar_redistribuicao_formal_sem_salto_de_fila_ou_orgao");
            draft.distributionActs().add("tratar_dependencia_prevencao_e_correcao_de_classe_na_porta_de_entrada");
            draft.distributionActs().add("encaminhar_redistribuicao_formal_com_rastro_de_unidade_origem_e_destino");
            draft.specializedFlows().add("protocolo_autuacao_prevencao_dependencia_e_redistribuicao");
            draft.managementActs().add("equalizar_porta_de_entrada_sem_romper_prevencao_juiz_natural_ou_dependencia");
            draft.forbiddenActs().add("protocolo_distribuicao_nao_encaminha_para_vara_estranha_sem_registro_formal");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.DISTRIBUTION_PORTAL_FOUNDATION);
        }
        if (support.containsToken(unitKind, "GABINETE")) {
            draft.unitTopology().add("bloco_funcional=GABINETE_E_CONCLUSAO");
            draft.operationalDomains().add("GABINETE_JUDICIAL_E_MINUTAS");
            draft.deskQueues().add("fila_conclusao_magistrado=" + support.normalize(fingerprint.groupingKey()));
            draft.deskQueues().add("fila_minutas_relatorios=" + support.normalize(fingerprint.specializationCluster()));
            draft.assignmentBoundaries().add("gabinete_nao_se_confunde_com_secretaria_ou_upj_compartilhada");
            draft.counterpartScopes().add("SECRETARIA_DA_UNIDADE_E_ASSESSORIA_DE_GABINETE");
            draft.assessorActs().add("preparar_pauta_de_julgamento_voto_ou_despacho_em_trilha_reservada_do_gabinete");
            draft.conclusionActs().add("priorizar_conclusoes_urgentes_modelos_e_pendencias_de_gabinete");
            draft.specializedFlows().add("gabinete_conclusao_minutas_precedentes_e_supervisao_judicial");
            draft.judgeOverrideActs().add("reordenar_conclusoes_e_priorizar_lote_urgente_do_gabinete");
            draft.managementActs().add("homologar_modelos_e_filas_reservadas_de_gabinete_sem_compartilhamento_irregular");
            draft.forbiddenActs().add("gabinete_nao_absorve_fila_cartoraria_comum_sem_trilha_especifica");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.GABINETE_FOUNDATION);
        }
        if (support.containsToken(unitKind, "UPJ")) {
            draft.unitTopology().add("bloco_funcional=UPJ_COMPARTILHADA_POR_ORIGEM");
            draft.operationalDomains().add("SERVICO_COMPARTILHADO_COM_ORIGEM_PRESERVADA");
            draft.deskQueues().add("fila_upj_por_vara_origem=" + support.normalize(fingerprint.groupingKey()));
            draft.deskQueues().add("fila_upj_contingencia=" + support.normalize(fingerprint.specializationCluster()));
            draft.assignmentBoundaries().add("upj_compartilha_serventia_sem_fundir_vinculo_processual_da_vara_origem");
            draft.counterpartScopes().add("VARAS_DE_ORIGEM_E_COORDENACAO_DA_UPJ");
            draft.secretariatActs().add("segmentar_fluxo_upj_por_vara_origem_sem_mistura_de_competencia");
            draft.conclusionActs().add("devolver_fluxo_upj_para_vara_origem_com_rastro_de_bloco_funcional");
            draft.specializedFlows().add("upj_servico_compartilhado_com_vara_origem_preservada");
            draft.managementActs().add("distribuir_capacidade_da_upj_por_bloco_de_origem_e_sla");
            draft.forbiddenActs().add("upj_nao_transforma_varas_de_origem_em_caixa_unica_sem_fronteira");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.UPJ_FOUNDATION);
        }
        if (support.containsToken(unitKind, "JUIZADO") || support.containsToken(judicialAxis, "JUIZADOS_ESPECIAIS")) {
            draft.unitTopology().add("bloco_funcional=JUIZADO_ESPECIAL");
            draft.operationalDomains().add("SESSAO_TERMO_E_CELERIDADE_DOS_JUIZADOS");
            draft.deskQueues().add("fila_sessoes_juizado=" + support.normalize(fingerprint.groupingKey()));
            draft.deskQueues().add("fila_termos_e_intimacoes_celeres=" + support.normalize(fingerprint.specializationCluster()));
            draft.assignmentBoundaries().add("juizado_preserva_rito_celere_sem_importar_fila_complexa_incompativel");
            draft.counterpartScopes().add("SECRETARIA_JUIZADO_E_PARTICIPANTES_DA_SESSAO");
            draft.secretariatActs().add("preparar_termo_ata_e_intimacao_celere_do_juizado");
            draft.specializedFlows().add("juizado_sessao_termo_intimacao_celere_e_execucao_enxuta");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.JUIZADO_FOUNDATION);
        }
        if (support.containsToken(unitKind, "SECRETARIA_SEGUNDO_GRAU") || support.containsToken(judicialAxis, "SEGUNDO_GRAU")) {
            draft.unitTopology().add("bloco_funcional=COLEGIADO_E_SESSAO_DE_SEGUNDO_GRAU");
            draft.operationalDomains().add("SESSAO_COLEGIADA_E_PREVENCAO_RECURSAL");
            draft.deskQueues().add("fila_pauta_colegiada=" + support.normalize(fingerprint.groupingKey()));
            draft.deskQueues().add("fila_prevencao_e_distribuicao_recursal=" + support.normalize(fingerprint.specializationCluster()));
            draft.deskQueues().add("fila_acordaos_e_publicacoes=" + support.normalize(fingerprint.varaCluster()));
            draft.assignmentBoundaries().add("secretaria_colegiada_nao_substitui_gabinete_do_relator_ou_orgao_fracionario_diverso");
            draft.counterpartScopes().add("GABINETE_DO_RELATOR_E_ORGAO_COLEGIADO");
            draft.secretariatActs().add("organizar_sessao_colegiada_prevencao_recursal_e_publicacao_de_acordao");
            draft.assessorActs().add("preparar_memoria_de_sessao_e_minuta_colegiada_sem_substituir_voto_do_relator");
            draft.conclusionActs().add("controlar_acordao_publicacao_intimacao_e_retorno_ao_orgao_fracionario");
            draft.specializedFlows().add("segundo_grau_prevencao_pauta_sessao_acordao_e_publicacao");
            draft.forbiddenActs().add("secretaria_de_segundo_grau_nao_redefine_prevencao_ou_composicao_colegiada_sem_trilha_formal");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.SECOND_DEGREE_FOUNDATION);
        }
        if (support.containsToken(unitKind, "CONTADORIA")) {
            draft.unitTopology().add("bloco_funcional=CALCULO_LIQUIDACAO_E_REQUISICOES");
            draft.operationalDomains().add("VERSOES_MEMORIA_E_RETORNO_TECNICO");
            draft.deskQueues().add("fila_memoria_calculo_e_versoes=" + support.normalize(fingerprint.groupingKey()));
            draft.assignmentBoundaries().add("contadoria_responde_em_trilha_tecnica_sem_substituir_orgao_decisor");
            draft.counterpartScopes().add("SECRETARIA_GABINETE_E_EXECUCAO");
            draft.specializedFlows().add("contadoria_memoria_liquidacao_requisicoes_e_retorno_tecnico");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.COUNTING_FOUNDATION);
        }
        if (support.containsToken(unitKind, "CENTRAL_MANDADOS")) {
            draft.unitTopology().add("bloco_funcional=CUMPRIMENTO_E_DILIGENCIA_EXTERNA");
            draft.deskQueues().add("fila_resultado_diligencia=" + support.normalize(fingerprint.groupingKey()));
            draft.assignmentBoundaries().add("central_mandados_separa_grupo_roteiro_resultado_e_devolucao_por_oficial");
            draft.expeditionActs().add("roteirizar_cumprimento_distribuir_oficial_e_registrar_resultado_de_mandado");
            draft.specializedFlows().add("mandados_diligencias_certidoes_e_devolucoes_por_oficial");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.MANDATE_CENTER_FOUNDATION);
        }
        if (support.containsToken(unitKind, "CEJUSC", "CENTRAL_AUDIENCIAS")) {
            draft.unitTopology().add("bloco_funcional=AUTOCOMPOSICAO_E_PRE_SESSAO");
            draft.operationalDomains().add("PRE_SESSAO_CONCILIACAO_E_TERMO");
            draft.deskQueues().add("fila_termos_e_retornos_cejusc=" + support.normalize(fingerprint.groupingKey()));
            draft.conclusionActs().add("devolver_termo_e_resultado_da_sessao_ao_juizo_de_origem");
            draft.specializedFlows().add("cejusc_pre_sessao_autocomposicao_termo_e_retorno_ao_juizo");
            draft.assignmentBoundaries().add("cejusc_devolve_ao_juizo_de_origem_sem_apagar_historia_do_feito_principal");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.CEJUSC_FOUNDATION);
        }
        if (processProfile == InstitutionalProcessProfile.COORDENADOR_UNIDADE) {
            draft.managementActs().add("governar_capacidade_da_unidade_com_leitura_de_bloco_funcional_e_sla");
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.COORDINATION_FOUNDATION);
        }
        if (draft.findings().isEmpty()) {
            draft.findings().add("mesa_operacional_segmentada_por_tipo_de_unidade_e_fronteira_real");
        }
    }
}
