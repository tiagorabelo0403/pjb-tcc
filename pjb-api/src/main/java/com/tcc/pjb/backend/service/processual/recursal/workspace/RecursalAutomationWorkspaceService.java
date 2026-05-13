package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalContrarrazoesBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalEffectsBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalChecklistBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalSectionLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalRetratacaoBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPoderRecorrerBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalTerminologyCatalog;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationCandidateView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursalAutomationWorkspaceService {

    private final RecursalAutomationService automationService;
    private final RecursalAutomationPlaybookService playbookService;

    public RecursalAutomationWorkspaceService(RecursalAutomationService automationService,
                                              RecursalAutomationPlaybookService playbookService) {
        this.automationService = automationService;
        this.playbookService = playbookService;
    }

    public RecursalAutomationWorkspaceResponse buildWorkspace(RecursalAutomationRequest request) {
        RecursalAutomationResponse response = automationService.advise(request);
        RecursalAutomationPlaybookResponse playbook = playbookService.buildPlaybook(request);
        String recursoPrincipal = playbook.rotaPrioritaria();
        List<RecursalAutomationWorkspaceTrackView> tracks = new ArrayList<>();
        if (response.poderRecorrerBloqueado()) {
            tracks.add(buildBlockedPowerTrack(response));
            return new RecursalAutomationWorkspaceResponse(
                    recursoPrincipal,
                    RecursalTerminologyCatalog.nomenclaturaAtiva(recursoPrincipal),
                    RecursalTerminologyCatalog.verbosOperacionais(recursoPrincipal),
                    true,
                    response.motivoBloqueioPoderRecorrer(),
                    List.copyOf(tracks)
            );
        }
        tracks.add(buildPriorityTrack(playbook));
        tracks.add(RecursalPanelHandoffTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalProceduralTaxonomyTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalCompetenceDistributionTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalAiDistributionWizardTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalAdjudicationWorkbenchTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalVisibilityLadderTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalBranchSegmentationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalCitizenPanelTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalRepresentationPanelTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalExternalOperationsPanelTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalAttorneyDashboardTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalDigitalCasefileTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalAttorneyAssociationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalOfficeCollaborationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalExternalCertificatesTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalInstitutionalOrganizationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalInstitutionalBoxesHistoryTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalMediaCollaborationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalAnalyticsIntelligenceTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalDeadlineNotificationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalNotificationAudienceTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalPostJudgmentEscalationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalCriticalAlertVisualTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalPetitioningReuseTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalBatchPetitioningTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalNationalRitesPetitioningTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalTribunalDifferentiationTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalInvolvedContextBoundaryTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalPanelContextSwitchTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalPanelRuntimeExperienceTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalActorContextExperienceTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalActorRiteTribunalPolicyTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalConcretePieceMatrixTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalExpeditionAndProceduralActsTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalSecretariatParityTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(RecursalSecretariatCapabilityMatrixTrackFactory.buildTrack(recursoPrincipal, request));
        tracks.add(buildFormalChecklistTrack(recursoPrincipal));
        tracks.addAll(RecursalGuidedPieceTrackFactory.buildTracks(recursoPrincipal, response, request, playbook));
        tracks.add(buildEffectsTrack(recursoPrincipal, request));
        if (RecursalRetratacaoBlueprint.trilhaPotencial(recursoPrincipal)) {
            tracks.add(buildRetratacaoTrack(recursoPrincipal));
        }
        if (request.recursoPrincipalInterposto()) {
            tracks.add(buildContrarrazoesTrack(response));
        }
        if (response.admiteRecursoAdesivo() && recursoPrincipal.equals("APELACAO")) {
            tracks.add(buildApelacaoAdesivaTrack(response));
        }
        if (mustOpenTribunalTrack(response, request)) {
            tracks.add(RecursalTribunalTrackFactory.buildDetailedTrack(recursoPrincipal, request));
        }
        return new RecursalAutomationWorkspaceResponse(
                recursoPrincipal,
                RecursalTerminologyCatalog.nomenclaturaAtiva(recursoPrincipal),
                RecursalTerminologyCatalog.verbosOperacionais(recursoPrincipal),
                false,
                response.motivoBloqueioPoderRecorrer(),
                List.copyOf(tracks)
        );
    }

    private RecursalAutomationWorkspaceTrackView buildPriorityTrack(RecursalAutomationPlaybookResponse playbook) {
        return new RecursalAutomationWorkspaceTrackView(
                "ROTA_PRIORITARIA_GUIADA",
                "Rota prioritária guiada",
                playbook.rotaPrioritaria(),
                playbook.secoesEssenciais().stream().toList(),
                List.of(
                        item("QUALIFICAR_DECISAO", "confirmar o tipo de pronunciamento e o recurso-base antes de abrir a peça", true),
                        item("AMARRAR_PRAZO", "fixar a contagem em dias úteis e travar a data-limite operacional", true),
                        item("CONSOLIDAR_SECOES", "montar a peça com as seções essenciais da rota prioritária", true)
                ),
                playbook.alertasCriticos()
        );
    }

    private RecursalAutomationWorkspaceTrackView buildBlockedPowerTrack(RecursalAutomationResponse response) {
        return new RecursalAutomationWorkspaceTrackView(
                "PODER_RECORRER_BLOQUEADO",
                "Poder de recorrer bloqueado",
                "PODER_RECORRER_BLOQUEADO",
                RecursalPoderRecorrerBlueprint.secoesMinimas(),
                List.of(
                        item("REGISTRAR_EVENTO", "registrar o fato impeditivo ou extintivo do poder de recorrer", true),
                        item("SUSTAR_INTERPOSICAO", "sustar abertura de nova peça enquanto o bloqueio persistir", true),
                        item("AUDITAR_COMPATIBILIDADE", "validar que não houve ato incompatível com o bloqueio já registrado", true)
                ),
                List.of(response.motivoBloqueioPoderRecorrer())
        );
    }

    private RecursalAutomationWorkspaceTrackView buildFormalChecklistTrack(String recursoPrincipal) {
        return new RecursalAutomationWorkspaceTrackView(
                "CHECKLIST_FORMAL_POR_PECA",
                "Checklist formal por peça",
                recursoPrincipal,
                RecursalFormalChecklistBlueprint.secoesPorRota(recursoPrincipal),
                RecursalFormalChecklistBlueprint.checklistDescricaoPorCodigo(recursoPrincipal).entrySet().stream()
                        .map(entry -> item(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.of("checklist formal conectado à rota prioritária " + recursoPrincipal)
        );
    }

    private RecursalAutomationWorkspaceTrackView buildEffectsTrack(String recursoPrincipal, RecursalAutomationRequest request) {
        List<String> alertas = RecursalEffectsBlueprint.efeitosProvaveis(recursoPrincipal, request.pretendeEfeitoInfringente()).stream()
                .map(effect -> "efeito provável: " + effect.name())
                .toList();
        return new RecursalAutomationWorkspaceTrackView(
                "EFEITOS_RECURSAIS_GUIADOS",
                "Efeitos recursais guiados",
                recursoPrincipal,
                RecursalEffectsBlueprint.secoesMinimas(recursoPrincipal),
                List.of(
                        item("MAPEAR_EFEITOS", "mapear os efeitos recursais mais prováveis da rota prioritária", true),
                        item("REVISAR_SUSPENSIVO", "validar necessidade de pedido de efeito suspensivo quando cabível", true),
                        item("TRAVAR_DEVOLUTIVIDADE", "identificar o alcance devolutivo antes do protocolo", true)
                ),
                alertas
        );
    }

    private RecursalAutomationWorkspaceTrackView buildRetratacaoTrack(String recursoPrincipal) {
        return new RecursalAutomationWorkspaceTrackView(
                "JUIZO_RETRATACAO_POTENCIAL",
                "Juízo de retratação potencial",
                recursoPrincipal,
                RecursalRetratacaoBlueprint.secoesMinimas(),
                RecursalRetratacaoBlueprint.passos().stream()
                        .map(code -> item(code, descricaoRetratacao(code), true))
                        .toList(),
                List.of("trilha potencial de retratação conectada ao cenário concreto e à rota " + recursoPrincipal)
        );
    }

    private RecursalAutomationWorkspaceTrackView buildContrarrazoesTrack(RecursalAutomationResponse response) {
        boolean admiteAdesivo = response.admiteRecursoAdesivo();
        List<String> alertas = new ArrayList<>();
        alertas.add(response.observacaoRecursoAdesivo());
        return new RecursalAutomationWorkspaceTrackView(
                "CONTRARRAZOES_RECURSAIS_GUIADAS",
                "Contrarrazões recursais guiadas",
                "CONTRARRAZOES",
                RecursalContrarrazoesBlueprint.secoes(admiteAdesivo),
                List.of(
                        item("CONFIRMAR_JANELA", "confirmar abertura da janela útil de contrarrazões", true),
                        item("IMPUGNAR_RECURSO_PRINCIPAL", "impugnar especificamente os fundamentos do recurso principal", true),
                        item("DEFINIR_ADESIVO", admiteAdesivo
                                ? "acoplar recurso adesivo na mesma janela útil das contrarrazões"
                                : "fechar resposta sem via adesiva adicional", true)
                ),
                List.copyOf(alertas)
        );
    }

    private RecursalAutomationWorkspaceTrackView buildApelacaoAdesivaTrack(RecursalAutomationResponse response) {
        return new RecursalAutomationWorkspaceTrackView(
                "APELACAO_ADESIVA_GUIADA",
                "Apelação adesiva guiada",
                "APELACAO",
                List.of(
                        RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                        RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                        RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                        RecursalFormalSectionLabels.CONTRARRAZOES,
                        RecursalFormalSectionLabels.SUCUMBENCIA_RECIPROCA,
                        RecursalFormalSectionLabels.SUBORDINACAO_RECURSO_PRINCIPAL,
                        RecursalFormalSectionLabels.RAZOES_RECURSAIS
                ),
                List.of(
                        item("SUCUMBENCIA_RECIPROCA", "confirmar sucumbência recíproca antes de abrir a via adesiva", true),
                        item("MESMA_JANELA_CONTRARRAZOES", "protocolar contrarrazões e adesivo dentro da mesma janela útil", true),
                        item("VINCULO_AO_PRINCIPAL", "registrar que o adesivo segue a sorte do recurso principal", true)
                ),
                List.of(response.observacaoRecursoAdesivo())
        );
    }

    private RecursalAutomationWorkspaceChecklistItemView item(String code, String description, boolean required) {
        return new RecursalAutomationWorkspaceChecklistItemView(code, description, required);
    }

    private boolean mustOpenTribunalTrack(RecursalAutomationResponse response, RecursalAutomationRequest request) {
        if (response.poderRecorrerBloqueado()) {
            return false;
        }
        if (request.desejaSustentacaoOral()) {
            return true;
        }
        return response.candidatos().stream()
                .map(RecursalAutomationCandidateView::recurso)
                .anyMatch(recurso -> recurso.equals("APELACAO")
                        || recurso.equals("RECURSO_INOMINADO")
                        || recurso.equals("AGRAVO_DE_INSTRUMENTO")
                        || recurso.equals("AGRAVO_INTERNO")
                        || recurso.equals("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO")
                        || recurso.equals("RECURSO_ESPECIAL")
                        || recurso.equals("RECURSO_EXTRAORDINARIO")
                        || recurso.equals("EMBARGOS_DIVERGENCIA"));
    }

    private String descricaoRetratacao(String code) {
        return switch (code) {
            case "CHECAR_RETRATACAO_POTENCIAL" -> "validar se o cenário concreto comporta juízo de retratação antes da remessa estabilizada";
            case "ISOLAR_CAPITULO_PASSIVEL_DE_REVISAO" -> "isolar o capítulo ou vício com maior potencial de retratação";
            case "VALIDAR_SE_HA_REENVIO_OU_MANTENCAO_DA_TRILHA" -> "decidir se a trilha segue para o tribunal ou se há recomposição interna prévia";
            case "AUDITAR_IMPACTO_NA_ROTA_PRIORITARIA" -> "auditar o impacto da retratação sobre a rota prioritária e a subsidiária";
            default -> "validar o passo " + code + " dentro da trilha de retratação";
        };
    }
}
