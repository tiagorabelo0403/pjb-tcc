package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import java.util.List;
import java.util.Objects;

final class InstitutionalOperationalDeskRoleActsAssembler {

    private final InstitutionalOperationalDeskSupport support;

    InstitutionalOperationalDeskRoleActsAssembler(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    void apply(InstitutionalOperationalDeskGovernanceDraft draft,
               InstitutionalOperationalDeskSnapshot snapshot) {
        if (snapshot.secretariatWorkflowEnabled()) {
            draft.secretariatActs().addAll(List.of(
                    "receber_expediente_de_secretaria",
                    "organizar_processos_por_unidade_e_vara",
                    "certificar_ato_ordinatorio",
                    "expedir_intimacao_ou_comunicacao_validada",
                    "preparar_conclusao_para_gabinete_ou_magistrado",
                    "controlar_prazos_e_cumprimentos_da_serventia",
                    "montar_pasta_operacional_da_audiencia",
                    "fechar_checklist_de_secretaria",
                    "autuar_reautuar_ou_retificar_classe_assunto_partes_em_fluxo_autorizado",
                    "limpar_filas_e_subfluxos_sem_perder_trilha_do_ato",
                    "marcar_publicacao_ciencia_ou_decurso_de_prazo",
                    "preparar_baixa_remessa_arquivamento_ou_desarquivamento_no_fluxo_autorizado"));
            if (snapshot.mandateWorkflowEnabled()) {
                draft.secretariatActs().add("expedir_mandado_ou_certidao_para_fluxo_de_cumprimento");
                draft.secretariatActs().add("expedir_oficio_carta_edital_ou_alvara_no_subfluxo_adequado");
            }
            if (snapshot.communicationWorkflowEnabled()) {
                draft.secretariatActs().add("gerir_comunicacoes_processuais_na_fila_da_unidade");
                draft.secretariatActs().add("confirmar_intimacoes_e_publicacoes_com_rastro_integral");
            }
            if (snapshot.distributionWorkflowEnabled()) {
                draft.secretariatActs().add("sanear_autuacao_prevencao_e_redistribuicao_sem_misturar_vara");
            }
        }
        if (snapshot.assessorWorkflowEnabled()) {
            draft.assessorActs().addAll(List.of(
                    "preparar_minuta_sem_substituir_assinatura_final",
                    "organizar_dossie_do_processo",
                    "conferir_documentos_e_representacoes",
                    "sugerir_encaminhamento_ao_titular",
                    "sinalizar_urgencia_ou_inconsistencia",
                    "preparar_nota_tecnica_ou_parecer_preliminar",
                    "preparar_relatorio_de_precedentes_memoria_processual_e_risco",
                    "conferir_prevencao_conexao_sigilo_e_prioridade",
                    "organizar_pauta_de_julgamento_ou_audiencia_para_gabinete"));
            if (snapshot.calculatorWorkflowEnabled()) {
                draft.assessorActs().add("preparar_memoria_de_calculo_ou_requisicao_tecnica");
            }
        }
        if (snapshot.magistrateOverrideEnabled()) {
            draft.judgeOverrideActs().addAll(List.of(
                    "corrigir_vinculo_de_unidade_em_situacao_excepcional",
                    "redesignar_ato_critico_com_rastro_integral",
                    "determinar_expedicao_imediata_em_urgencia",
                    "avocar_fluxo_para_gabinete",
                    "sanear_erro_material_de_pauta_ou_fila"));
        }
        if (snapshot.management() || snapshot.magistrateProfile()) {
            draft.managementActs().addAll(List.of(
                    "homologar_lotacao_ou_cobertura",
                    "redistribuir_filas_da_unidade_sem_romper_autoria",
                    "abrir_janela_operacional_para_pauta",
                    "acionar_contingencia_da_unidade",
                    "fechar_auditoria_de_trilha_operacional",
                    "parametrizar_tempo_padrao_de_audiencia_por_orgao_ou_sala",
                    "segmentar_grupos_operacionais_sem_misturar_varas",
                    "homologar_saneamento_estrutural_de_filas_e_subfluxos"));
        }
        if (snapshot.batchWorkflowEnabled()) {
            draft.managementActs().add("autorizar_operacao_em_lote_com_trilha_forte");
        }
        if (snapshot.prisonFlow()) {
            draft.managementActs().add("sincronizar_apresentacao_custodial_com_unidade_prisional");
        }
        if (snapshot.distributionWorkflowEnabled()) {
            draft.distributionActs().addAll(List.of(
                    "registrar_prevencao_dependencia_redistribuicao_formal",
                    "validar_autuacao_classe_assunto_e_orgao_destino",
                    "preservar_juiz_natural_e_historia_da_porta_de_entrada",
                    "encaminhar_para_unidade_competente_sem_salto_de_fila"));
        }
        if (snapshot.expeditionWorkflowEnabled()) {
            draft.expeditionActs().addAll(List.of(
                    "expedir_mandado_oficio_carta_edital_ou_alvara_no_fluxo_competente",
                    "controlar_resultado_de_cumprimento_devolucao_e_certidao",
                    "registrar_publicacao_intimacao_citacao_e_decurso_com_rastro_integral"));
        }
        if (snapshot.conclusionWorkflowEnabled()) {
            draft.conclusionActs().addAll(List.of(
                    "preparar_conclusao_voto_minuta_ou_sentenca_em_trilha_reservada",
                    "devolver_conclusao_com_orientacao_e_rastro_reservado",
                    "controlar_retorno_publicacao_e_baixa_do_ato_decisorio"));
        }
        if (snapshot.assessorWorkflowEnabled()) {
            draft.forbiddenActs().add("assessoria_nao_assina_ato_final_sem_fluxo_do_titular");
        }
        if (snapshot.secretariatWorkflowEnabled()) {
            draft.forbiddenActs().add("secretaria_nao_decide_competencia_material_ou_merito");
            draft.forbiddenActs().add("secretaria_nao_remove_sigilo_ou_altera_polo_sem_fluxo_autorizado");
        }
        if (snapshot.triageWorkflowEnabled()) {
            draft.forbiddenActs().add("triagem_nao_substitui_secretaria_ou_titular");
        }
        if (snapshot.calculatorWorkflowEnabled()) {
            draft.forbiddenActs().add("calculadora_nao_redefine_valor_sem_trilha_do_responsavel");
        }
        if (snapshot.prisonFlow()) {
            draft.forbiddenActs().add("fluxo_prisional_nao_altera_pauta_sem_ordem_ou_coordenacao_validada");
        }
        if (snapshot.mandateWorkflowEnabled() && support.containsToken(snapshot.unitKind(), "CENTRAL_MANDADOS")) {
            draft.forbiddenActs().add("central_mandados_nao_redefine_competencia_ou_pauta_do_orgao");
        }
    }
}
