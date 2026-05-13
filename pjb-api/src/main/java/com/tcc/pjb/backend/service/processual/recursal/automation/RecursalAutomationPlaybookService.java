package com.tcc.pjb.backend.service.processual.recursal.automation;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAdjudicationWorkbenchBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAiDistributionWizardBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAttorneyDashboardBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAttorneyAssociationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalDigitalCasefileBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalOfficeCollaborationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalBatchPetitioningBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalExternalCertificatesBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalInstitutionalBoxesHistoryBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalMediaCollaborationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAnalyticsIntelligenceBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalActorContextExperienceBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalActorRiteTribunalPolicyBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalBranchSegmentationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalCitizenPanelBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalCompetenceDistributionBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalCriticalAlertVisualBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalExternalOperationsPanelBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalInstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalContrarrazoesBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalDeadlineNotificationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalEffectsBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalExpeditionAndProceduralActsBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalChecklistBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalGuidedPieceBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalInvolvedContextBoundaryBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalJurisdictionPanelBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPetitioningReuseBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNotificationAudienceBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNationalRitesPetitioningBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalConcretePieceMatrixBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalTribunalDifferentiationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPanelContextSwitchBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPanelRuntimeExperienceBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalProceduralTaxonomyBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPostJudgmentEscalationBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalRetratacaoBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalRepresentationPanelBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalSecretariatCapabilityMatrixBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalSecretariatParityBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalVisibilityLadderBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationCandidateView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookStepView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursalAutomationPlaybookService {

    private final RecursalAutomationService automationService;

    public RecursalAutomationPlaybookService(RecursalAutomationService automationService) {
        this.automationService = automationService;
    }

    public RecursalAutomationPlaybookResponse buildPlaybook(RecursalAutomationRequest request) {
        RecursalAutomationResponse response = automationService.advise(request);
        RecursalAutomationCandidateView principal = response.candidatos().getFirst();
        RecursalAutomationCandidateView alternativa = response.candidatos().size() > 1 ? response.candidatos().get(1) : null;
        List<String> alertas = response.sinais().stream()
                .filter(signal -> !signal.status().equals("OK"))
                .map(signal -> signal.codigo() + ": " + signal.mensagem())
                .toList();
        List<RecursalAutomationPlaybookStepView> passos = buildSteps(request, response, principal, alternativa);
        return new RecursalAutomationPlaybookResponse(
                principal.recurso(),
                alternativa == null ? null : alternativa.recurso(),
                principal.prazoDiasUteis(),
                principal.juizoAdmissibilidadeCompetencia(),
                principal.secoesObrigatorias(),
                alertas,
                passos
        );
    }

    private List<RecursalAutomationPlaybookStepView> buildSteps(RecursalAutomationRequest request,
                                                                RecursalAutomationResponse response,
                                                                RecursalAutomationCandidateView principal,
                                                                RecursalAutomationCandidateView alternativa) {
        List<RecursalAutomationPlaybookStepView> steps = new ArrayList<>();
        int order = 1;
        steps.add(step(order++, "QUALIFICAR_PRONUNCIAMENTO", "confirmar o tipo de pronunciamento judicial antes da peça recursal"));
        if (response.poderRecorrerBloqueado()) {
            steps.add(step(order++, "REGISTRAR_FATO_EXTINTIVO", response.motivoBloqueioPoderRecorrer()));
            steps.add(step(order, "SUSTAR_INTERPOSICAO", "bloquear a abertura de nova peça recursal e registrar o evento no workspace"));
            return steps;
        }
        if (principal.recurso().equals("IRRECORRIVEL")) {
            steps.add(step(order, "BLOQUEAR_INTERPOSICAO", "suspender a interposição e revisar se houve apenas despacho de mero expediente"));
            return steps;
        }
        steps.add(step(order++, "FIXAR_ROTA_PRIORITARIA", "assumir " + principal.recurso() + " como trilha prioritária do cenário"));
        steps.add(step(order++, "ALINHAR_TAXONOMIA_CNJ_E_TIPO_PETICAO", RecursalProceduralTaxonomyBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "RESOLVER_COMPETENCIA_E_DISTRIBUICAO", RecursalCompetenceDistributionBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ATIVAR_WIZARD_DISTRIBUICAO_ASSISTIDA_IA", RecursalAiDistributionWizardBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ABRIR_PECA_ESPECIFICA", descricaoPecaGuiada(principal.recurso())));
        if (response.admiteRecursoAdesivo()) {
            steps.add(step(order++, "ACOPLAR_ADESIVO", "interpor contrarrazões e recurso adesivo dentro da mesma janela útil"));
        } else if (request.contrarrazoesJaProtocoladas()) {
            steps.add(step(order++, "VEDAR_ADESIVO", "não abrir trilha adesiva porque as contrarrazões já foram protocoladas"));
        } else {
            steps.add(step(order++, "CHECAR_ADESIVO", response.observacaoRecursoAdesivo()));
        }
        steps.add(step(order++, "VALIDAR_CHECKLIST_FORMAL", "revisar checklist formal da peça prioritária: "
                + RecursalFormalChecklistBlueprint.checklistCodigos(principal.recurso())));
        steps.add(step(order++, "DIRECIONAR_PAINEL_COMPETENTE", descricaoPainelJuridicional(principal.recurso(), request)));
        steps.add(step(order++, "CONECTAR_MALHA_PAINEIS_WORKBENCHES", RecursalAdjudicationWorkbenchBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_ESCADA_VISIBILIDADE", RecursalVisibilityLadderBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "SEGMENTAR_POR_RAMO_RITO_SIGILO", RecursalBranchSegmentationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO", RecursalCitizenPanelBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ABRIR_PAINEL_RECURSAL_PARTES_REPRESENTANTES", RecursalRepresentationPanelBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_PAINEL_EXTERNO_OPERACIONAL", RecursalExternalOperationsPanelBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "PUBLICAR_PAINEL_ADVOGADO_RECURSAL_COMPLETO", RecursalAttorneyDashboardBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "APROFUNDAR_AUTOS_DIGITAIS_RECURSAIS", RecursalDigitalCasefileBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_HABILITACAO_E_ASSOCIACAO_RECURSAL", RecursalAttorneyAssociationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_ESCRITORIO_ASSISTENTES_E_SUBSTABELECIMENTO", RecursalOfficeCollaborationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "EMITIR_CERTIDOES_EXTERNAS_E_EXECUTIVAS", RecursalExternalCertificatesBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_REPRESENTACAO_E_CAIXAS_INSTITUCIONAIS", RecursalInstitutionalOrganizationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "REFORCAR_CAIXAS_HISTORICO_E_DEVOLUCAO_INSTITUCIONAL", RecursalInstitutionalBoxesHistoryBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ATIVAR_COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL", RecursalMediaCollaborationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_OBSERVABILIDADE_INDEXACAO_E_AVISOS_MOVEIS", RecursalAnalyticsIntelligenceBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_ALERTAS_PRAZO_E_NOTIFICACOES", RecursalDeadlineNotificationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ESCALONAR_ALERTAS_POR_PERFIL_E_CRITICIDADE", RecursalNotificationAudienceBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_POS_JULGAMENTO_RECURSAL", RecursalPostJudgmentEscalationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ATIVAR_ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS", RecursalCriticalAlertVisualBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "REUSAR_STUDIO_E_JORNADA_PETICIONAMENTO_RECURSAL", RecursalPetitioningReuseBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_PETICIONAMENTO_LOTE_E_ASSINATURA_LOTE", RecursalBatchPetitioningBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "DIFERENCIAR_PETICIONAMENTO_POR_RITO_E_ESPECIE", RecursalNationalRitesPetitioningBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "DIFERENCIAR_TRIBUNAL_ORGAO_PRAZO_E_FILTROS", RecursalTribunalDifferentiationBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "LIMITAR_COMUTACAO_A_ENVOLVIDOS_E_PRESERVAR_BUSCA_NEUTRA", RecursalInvolvedContextBoundaryBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "COMUTAR_PAINEIS_POR_RITO_TRIBUNAL_E_PERFIL", RecursalPanelContextSwitchBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "AJUSTAR_VOCABULARIO_CARDS_ATALHOS_E_DETALHES", RecursalPanelRuntimeExperienceBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS", RecursalActorContextExperienceBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "APLICAR_MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL", RecursalActorRiteTribunalPolicyBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO", RecursalConcretePieceMatrixBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "ORQUESTRAR_MALOTES_PETICIONAMENTO_E_ATOS_RECURSAIS", RecursalExpeditionAndProceduralActsBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "REFORCAR_SECRETARIA_MULTIGRAU", RecursalSecretariatParityBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "PERSISTIR_MATRIZ_CAPACIDADES_SECRETARIA", RecursalSecretariatCapabilityMatrixBlueprint.descricaoExecutiva(principal.recurso(), request)));
        steps.add(step(order++, "REUSAR_SUPERFICIES_EXISTENTES", RecursalSecretariatCapabilityMatrixBlueprint.descricaoConexaoComSuperficiesExistentes(principal.recurso(), request)));
        steps.add(step(order++, "ANALISAR_EFEITOS_RECURSAIS", "mapear os efeitos recursais prováveis da rota prioritária: "
                + RecursalEffectsBlueprint.efeitosProvaveis(principal.recurso(), request.pretendeEfeitoInfringente())));
        if (RecursalRetratacaoBlueprint.trilhaPotencial(principal.recurso())) {
            steps.add(step(order++, "CHECAR_RETRATACAO_POTENCIAL", "validar incidência de juízo de retratação potencial para a rota prioritária"));
        }
        if (request.recursoPrincipalInterposto()) {
            steps.add(step(order++, "PREPARAR_CONTRARRAZOES", "abrir trilha de contrarrazões com seções: "
                    + RecursalContrarrazoesBlueprint.secoes(response.admiteRecursoAdesivo())));
        }
        if (request.inadmissaoRecursoExcepcional()) {
            steps.add(step(order++, "IMPUGNAR_INADMISSAO", "estruturar impugnação específica da inadmissão do recurso excepcional"));
        } else {
            steps.add(step(order++, "MONTAR_SECOES", "consolidar as seções obrigatórias da peça prioritária"));
        }
        if (request.divergenciaJurisprudencialInterna()) {
            steps.add(step(order++, "ANEXAR_PARADIGMA", "demonstrar divergência analítica e vincular acórdão paradigma idôneo"));
        }
        if (request.preparoInsuficiente()) {
            steps.add(step(order++, "COMPLEMENTAR_PREPARO", "corrigir preparo insuficiente dentro da janela legal de complementação"));
        } else if (!request.preparoEfetuado() && !request.autosEletronicos()) {
            steps.add(step(order++, "VALIDAR_PREPARO", "confirmar preparo ou hipótese legal de dispensa antes do protocolo"));
        }
        if (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado()) {
            steps.add(step(order++, "COMPROVAR_FERIADO_LOCAL", "juntar prova do feriado local no próprio ato de interposição"));
        }
        steps.add(step(order++, "TRILHA_TRIBUNAL", descricaoTrilhaTribunal(principal.recurso(), request)));
        if (request.desejaSustentacaoOral()) {
            steps.add(step(order++, "PREPARAR_SUSTENTACAO_ORAL", "reservar janela de sustentação oral e sincronizar a pauta com o time recursal"));
        }
        if (alternativa != null) {
            steps.add(step(order, "ROTA_SUBSIDIARIA", "manter " + alternativa.recurso() + " como trilha subsidiária sob revisão técnica"));
        }
        return steps;
    }

    private String descricaoPecaGuiada(String recurso) {
        if (!RecursalGuidedPieceBlueprint.supported(recurso)) {
            return "estruturar a peça prioritária com base no checklist formal e nas seções mínimas já resolvidas pelo cenário";
        }
        return "abrir a peça específica guiada de " + recurso + " com checklist operacional próprio e sem espalhar requisitos fora do eixo recursal";
    }

    private String descricaoPainelJuridicional(String recurso, RecursalAutomationRequest request) {
        String base = "encaminhar a atuação decisória para "
                + RecursalJurisdictionPanelBlueprint.painelDestino(recurso, request)
                + ", sob competência de "
                + RecursalJurisdictionPanelBlueprint.orgaoJulgador(recurso, request);
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recurso, request)) {
            return base + ", sem redistribuição artificial para outro juiz ou outro órgão externo";
        }
        return base + ", preservando apenas leitura, histórico e apoio no painel de origem";
    }

    private String descricaoTrilhaTribunal(String recurso, RecursalAutomationRequest request) {
        String base;
        if (request.juizadoEspecial() && recurso.equals("RECURSO_INOMINADO")) {
            base = "acompanhar recebimento no juizado, distribuição interna, turma recursal, pauta e publicação do acórdão ou súmula colegiada";
        } else {
            base = "acompanhar recebimento no tribunal, triagem de distribuição, relatoria, pauta, julgamento e publicação do acórdão";
        }
        if (request.desejaSustentacaoOral()) {
            base += ", com reserva de sustentação oral e preparação para a sessão";
        }
        if (recurso.equals("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO")
                || recurso.equals("EMBARGOS_DIVERGENCIA")
                || recurso.equals("RECURSO_ESPECIAL")
                || recurso.equals("RECURSO_EXTRAORDINARIO")) {
            base += ", preservando o filtro de presidência ou vice-presidência do tribunal recorrido e a eventual subida à corte superior";
        }
        return base;
    }

    private RecursalAutomationPlaybookStepView step(int order, String code, String description) {
        return new RecursalAutomationPlaybookStepView(order, code, description);
    }
}
