package com.tcc.pjb.backend.service.processual.comunicacao.institutional.support;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalJudiciaryPopulationSizing;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelProvisioningReadinessApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalHearingRiteGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalOperationalDeskGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelProvisioningReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceProfileResponse;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalFacadeSupport {

    private final InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService;

    public NationalCommunicationInstitutionalFacadeSupport(InstitutionalPanelProvisioningReadinessApplicationService panelProvisioningReadinessApplicationService) {
        this.panelProvisioningReadinessApplicationService = panelProvisioningReadinessApplicationService;
    }

    public DestinatarioInstitucionalKind parseDestinatarioKind(String raw) {
        return raw == null || raw.isBlank() ? null : DestinatarioInstitucionalKind.fromTexto(raw);
    }

    public InstitutionalOrganizationScope parseOrganizationScope(String raw) {
        return raw == null || raw.isBlank() ? null : InstitutionalOrganizationScope.fromTexto(raw);
    }

    public InstitutionalAccessLaneKind parseAccessLaneKind(String raw) {
        return raw == null || raw.isBlank() ? null : InstitutionalAccessLaneKind.fromTexto(raw);
    }

    public InstitutionalNominationRole parseNominationRole(String raw) {
        return parseEnum(raw, InstitutionalNominationRole.class);
    }

    public InstitutionalTrustLevel parseTrustLevel(String raw) {
        return parseEnum(raw, InstitutionalTrustLevel.class);
    }

    public FuncaoOperacionalInstitucional parseFuncaoOperacional(String raw) {
        return parseEnum(raw, FuncaoOperacionalInstitucional.class);
    }

    public InstitutionalProcessProfile parseProcessProfile(String raw) {
        return parseEnum(raw, InstitutionalProcessProfile.class);
    }

    public InstitutionalEntryLandingPanel parseLandingPanel(String raw) {
        return parseEnum(raw, InstitutionalEntryLandingPanel.class);
    }

    public Set<CapacidadeCaixaInstitucional> parseCapacidades(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> CapacidadeCaixaInstitucional.valueOf(item.trim().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CapacidadeCaixaInstitucional.class)));
    }

    public NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse toResponse(InstitutionalJudiciaryPopulationSizing item) {
        return new NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse(
                item.tribunaisNacionais(),
                item.magistradosAtivosBaseline(),
                item.servidoresAtivosBaseline(),
                item.usuariosInternosCoreBaseline(),
                item.afiliacoesInstitucionaisAtivasModeladas(),
                item.nomeacoesAtivasModeladas(),
                item.contextosInstitucionaisAtivosModelados(),
                item.picoSessoesConcorrentesPlanejado(),
                item.replicasLeituraRegionaisMinimas(),
                item.bucketsParticionamentoEscritaMinimos(),
                item.eixosParticionamento(),
                item.segmentosInstitucionaisCobertos(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse toResponse(InstitutionalHorizontalDataPlanePlan item) {
        return new NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse(
                item.profileKey(),
                item.affiliationId(),
                item.nominationId(),
                item.organizationScope(),
                item.destinatarioKind(),
                item.requestedMunicipality(),
                item.requestedUf(),
                item.responsibleTribunalCode(),
                item.responsibleUnitCode(),
                item.responsibleUnitName(),
                item.responsibleComarca(),
                item.caixaCodigo(),
                item.panelCode(),
                item.landingPath(),
                item.readyForInstitutionalPanel(),
                item.routeToPersonalPanel(),
                item.localUnitPresent(),
                item.coverageMode(),
                item.horizontalDataPlaneKey(),
                item.primaryWritePartitionKey(),
                item.readReplicaCode(),
                item.writeShardBucket(),
                item.writeShardBucketCount(),
                item.warmArchivePartitionKey(),
                item.partitionAxes(),
                item.routingHeaders(),
                item.requiredApprovals(),
                item.approvedApprovals(),
                item.pendingApprovals(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalOperationalProfileResponse toResponse(InstitutionalOperationalProfileProjection item) {
        if (item == null) {
            return null;
        }
        InstitutionalPanelProvisioningReadiness panelProvisioning = panelProvisioningReadinessApplicationService.avaliar(item);
        return new NationalCommunicationInstitutionalOperationalProfileResponse(
                item.profileKey(),
                item.profileState(),
                item.visibleInPjb(),
                item.affiliationId(),
                item.nominationId(),
                item.nominatedUserId(),
                item.nominatedUserName(),
                item.tipoUsuario(),
                item.organizationScope(),
                item.destinatarioKind(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.caixaCodigo(),
                item.accessLaneKind(),
                item.nominationRole(),
                item.funcaoOperacional(),
                item.processProfile(),
                item.panelCode(),
                item.landingPath(),
                item.accentColor(),
                item.processAreaCode(),
                item.trustFloor(),
                item.activeNomination(),
                item.fullyApproved(),
                item.readyForInstitutionalPanel(),
                item.routeToPersonalPanel(),
                item.directPersonalAccessAvailable(),
                item.localUnitPresent(),
                item.coverageMode(),
                item.responsibleTribunalCode(),
                item.responsibleUnitCode(),
                item.responsibleUnitName(),
                item.responsibleComarca(),
                item.horizontalDataPlaneKey(),
                item.primaryWritePartitionKey(),
                item.readReplicaCode(),
                item.capacidades(),
                item.requiredApprovals(),
                item.approvedApprovals(),
                item.pendingApprovals(),
                item.findings(),
                item.fundamentos(),
                toResponse(panelProvisioning),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalPanelProvisioningReadinessResponse toResponse(InstitutionalPanelProvisioningReadiness item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalPanelProvisioningReadinessResponse(
                item.profileCode(),
                item.panelCode(),
                item.initialRoute(),
                item.blueprintMatched(),
                item.workspaceBound(),
                item.routeReady(),
                item.sectionsReady(),
                item.quickActionsReady(),
                item.guardsReady(),
                item.visibilityRulesReady(),
                item.tabsReady(),
                item.workspaceActionsReady(),
                item.authorityBandsReady(),
                item.separatorsReady(),
                item.notificationsReady(),
                item.calendarReady(),
                item.hearingsReady(),
                item.readingModeReady(),
                item.triageReady(),
                item.presentationReady(),
                item.colorSystemReady(),
                item.opinionFlowReady(),
                item.calculatorReady(),
                item.sharedExperienceReady(),
                item.complete(),
                item.totalBlueprints(),
                item.totalPrimarySections(),
                item.totalQuickActions(),
                item.totalSecurityGuards(),
                item.totalVisibilityRules(),
                item.totalTabs(),
                item.totalWorkspaceActions(),
                item.totalAuthorityBands(),
                item.totalSeparators(),
                item.totalSharedExperienceSurfaces(),
                item.totalSharedExperienceSurfacesReady(),
                item.primarySections(),
                item.quickActions(),
                item.securityGuards(),
                item.visibilityRules(),
                item.tabs(),
                item.readySharedExperienceSurfaces(),
                item.missingSharedExperienceSurfaces(),
                item.findings(),
                item.fundamentos(),
                toResponse(item.hearingGovernance()),
                toResponse(item.deskGovernance()),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalOperationalDeskGovernanceResponse toResponse(InstitutionalOperationalDeskGovernance item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalOperationalDeskGovernanceResponse(
                item.sectionVisible(),
                item.unitScopeBound(),
                item.segregatedByTribunal(),
                item.segregatedByComarca(),
                item.segregatedByUnit(),
                item.segregatedByVaraOrSpecialization(),
                item.magistrateOverrideEnabled(),
                item.secretariatWorkflowEnabled(),
                item.assessorWorkflowEnabled(),
                item.triageWorkflowEnabled(),
                item.mandateWorkflowEnabled(),
                item.communicationWorkflowEnabled(),
                item.opinionWorkflowEnabled(),
                item.calculatorWorkflowEnabled(),
                item.batchWorkflowEnabled(),
                item.distributionWorkflowEnabled(),
                item.expeditionWorkflowEnabled(),
                item.conclusionWorkflowEnabled(),
                item.queueManagementWorkflowEnabled(),
                item.organizationalScopeKey(),
                item.territorialScopeKey(),
                item.unitGroupingKey(),
                item.operationalIsolationMode(),
                item.judicialAxis(),
                item.unitKind(),
                item.assignmentBoundaryKey(),
                item.unitTopology(),
                item.operationalDomains(),
                item.deskQueues(),
                item.assignmentBoundaries(),
                item.counterpartScopes(),
                item.secretariatActs(),
                item.assessorActs(),
                item.judgeOverrideActs(),
                item.managementActs(),
                item.distributionActs(),
                item.expeditionActs(),
                item.conclusionActs(),
                item.specializedFlows(),
                item.forbiddenActs(),
                item.findings(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse toResponse(InstitutionalHearingSchedulingGovernance item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalHearingSchedulingGovernanceResponse(
                item.sectionVisible(),
                item.canRequestHearing(),
                item.canSuggestSlot(),
                item.canOrganizeDocket(),
                item.canOperationallySchedule(),
                item.canReschedule(),
                item.canCancel(),
                item.canReserveRoom(),
                item.canManageVirtualRoom(),
                item.canConfirmAttendance(),
                item.canRecordTerm(),
                item.canIssueHearingCommunications(),
                item.canPrepareHearingBundle(),
                item.requiresUnitIsolation(),
                item.requiresJudicialAuthorization(),
                item.requiresSecretariatCoordination(),
                item.schedulingScopeKey(),
                item.operationalQueues(),
                item.segregationGuards(),
                item.oversightActors(),
                item.allowedRiteGroups(),
                item.riteGovernances().stream().map(this::toResponse).toList(),
                item.forbiddenActs(),
                item.findings(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalHearingRiteGovernanceResponse toResponse(InstitutionalHearingRiteGovernance item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalHearingRiteGovernanceResponse(
                item.riteCode(),
                item.justiceBranch(),
                item.jurisdictionAxis(),
                item.specializationAxis(),
                item.hearingKind(),
                item.sectionVisible(),
                item.canRequestHearing(),
                item.canSuggestSlot(),
                item.canOperationallySchedule(),
                item.canReschedule(),
                item.canCancel(),
                item.canReserveRoom(),
                item.canManageVirtualRoom(),
                item.canConfirmAttendance(),
                item.canRecordTerm(),
                item.canIssueHearingCommunications(),
                item.canPrepareHearingBundle(),
                item.canOnlyTrack(),
                item.requiresUnitIsolation(),
                item.requiresJudicialAuthorization(),
                item.requiresSecretariatCoordination(),
                item.queueScopeKey(),
                item.allowedActs(),
                item.forbiddenActs(),
                item.requestActors(),
                item.preparatoryActors(),
                item.communicationActors(),
                item.operationalActors(),
                item.trackingActors(),
                item.oversightActors(),
                item.segregationGuards(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalEntryActivationResponse toResponse(InstitutionalEntryActivationDecision item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalEntryActivationResponse(
                item.userId(),
                item.userName(),
                item.affiliationId(),
                item.nominationId(),
                item.profileKey(),
                item.profileState(),
                item.targetEnvironment(),
                item.entryMode(),
                item.contextId(),
                item.panelCode(),
                item.landingPath(),
                item.processAreaCode(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.horizontalDataPlaneKey(),
                item.readReplicaCode(),
                item.sessionRiskLevel(),
                item.sessionRiskScore(),
                item.govBrNivelGarantia(),
                item.recommendedSensitiveAct(),
                item.stepUpStartPath(),
                item.institutionalProfileVisible(),
                item.directInstitutionalContextAvailable(),
                item.activateInstitutionalContext(),
                item.panelProvisioningComplete(),
                item.sharedExperienceReady(),
                item.requiresPanelProvisioningReview(),
                item.routeToPersonalPanel(),
                item.blocked(),
                item.requiresGovBrBinding(),
                item.requiresTrustedDevice(),
                item.requiresStepUp(),
                item.requiresQualifiedCertificate(),
                item.requiresInstitutionalNetwork(),
                item.acceptsRemoteCertificateAuthorization(),
                item.requiresManualApproval(),
                item.panelProvisioningFindings(),
                item.blockers(),
                item.warnings(),
                item.garantias(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse toResponse(InstitutionalTrustGovernanceProfile item) {
        return new NationalCommunicationInstitutionalTrustGovernanceProfileResponse(
                item.profileKey(),
                item.affiliationId(),
                item.nominationId(),
                item.nominatedUserId(),
                item.nominatedUserName(),
                item.tipoUsuario(),
                item.organizationScope(),
                item.destinatarioKind(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.panelCode(),
                item.landingPath(),
                item.accentColor(),
                item.processAreaCode(),
                item.trustFloor(),
                item.requiresStepUp(),
                item.requiresCertificate(),
                item.requiresInstitutionalNetwork(),
                item.directPersonalAccessAvailable(),
                item.judicialFlowSensitive(),
                item.requiredApprovals(),
                item.approvedApprovals(),
                item.pendingApprovals(),
                item.fullyApproved(),
                item.readyForInstitutionalPanel(),
                item.routeToPersonalPanel(),
                item.horizontalDataPlaneKey(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalAffiliationResponse toAffiliation(InstitutionalAffiliation item) {
        return new NationalCommunicationInstitutionalAffiliationResponse(
                item.affiliationId(),
                item.destinatarioKind().name(),
                item.organizationScope() == null ? null : item.organizationScope().name(),
                item.blueprintCode(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.uf(),
                item.comarca(),
                item.cnpj(),
                item.esferaAdministrativa(),
                item.ramosMateriais(),
                item.abrangenciasTerritoriais(),
                item.dominioInstitucional(),
                item.autoridadeAderenteCargo(),
                item.representanteUsuarioId(),
                item.representativeRole() == null ? null : item.representativeRole().name(),
                item.canaisHabilitados(),
                item.politicaCiencia(),
                item.sla(),
                item.regrasFallback(),
                item.conveniosIntegracoes(),
                item.trustFloor() == null ? null : item.trustFloor().name(),
                item.requerDuplaAprovacaoAdministrador(),
                item.requerCertificadoICP(),
                item.restringeCertificadoRedeInstitucional(),
                item.permiteUsoRemotoComAutorizacao(),
                item.status().name(),
                item.fundamentos(),
                item.createdAt(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalNominationResponse toNomination(InstitutionalNomination item,
                                                                             NationalCommunicationInstitutionalOperationalProfileResponse operationalProfile) {
        return new NationalCommunicationInstitutionalNominationResponse(
                item.nominationId(),
                item.affiliationId(),
                item.nominatedUserId(),
                item.nominatedUserName(),
                item.tipoUsuario() == null ? null : item.tipoUsuario().name(),
                item.accessLaneKind() == null ? null : item.accessLaneKind().name(),
                item.nominationRole().name(),
                item.funcaoOperacional().name(),
                item.processProfile().name(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.capacidades().stream().map(Enum::name).toList(),
                item.trustFloor() == null ? null : item.trustFloor().name(),
                item.panelPreferencial() == null ? null : item.panelPreferencial().name(),
                item.status().name(),
                item.ativaDe(),
                item.ativaAte(),
                item.requerStepUpMfa(),
                item.requerCertificadoICP(),
                item.requerRedeInstitucional(),
                item.permiteUsoRemotoAutorizado(),
                operationalProfile,
                item.createdAt(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalIdentityBaseProfileResponse toIdentityBase(InstitutionalIdentityBaseProfile item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalIdentityBaseProfileResponse(
                item.identityCode(),
                item.tipoUsuarioBase() == null ? null : item.tipoUsuarioBase().name(),
                item.possuiFluxoDireto(),
                item.entryModePreferencial() == null ? null : item.entryModePreferencial().name(),
                item.processProfileBase() == null ? null : item.processProfileBase().name(),
                item.painelBase() == null ? null : item.painelBase().name(),
                item.trustFloorBase() == null ? null : item.trustFloorBase().name(),
                item.exigeNomeacaoInstitucionalParaAtos(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalTrustAssessmentResponse toAssessment(InstitutionalTrustAssessment item) {
        return new NationalCommunicationInstitutionalTrustAssessmentResponse(
                item.userId(),
                item.userName(),
                item.entryMode().name(),
                item.affiliationId(),
                item.nominationId(),
                item.trustLevel().name(),
                item.factors().stream().map(Enum::name).toList(),
                item.trustedInstitutionalNetwork(),
                item.managedInstitutionalLogin(),
                item.remoteCertificateAuthorizationActive(),
                item.certificadoPermitidoNaSessao(),
                item.mfaAtivo(),
                item.autorizado(),
                item.panelPreferencial() == null ? null : item.panelPreferencial().name(),
                item.reasons(),
                item.evaluatedAt());
    }

    public NationalCommunicationInstitutionalEntryContextResponse toContext(InstitutionalEntryContext item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalEntryContextResponse(
                item.contextId(),
                item.destinatarioKind().name(),
                item.organizacaoKind() == null ? null : item.organizacaoKind().name(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.nucleo(),
                item.uf(),
                item.comarca(),
                item.caixaCodigo(),
                item.caixaNome(),
                item.processProfile().name(),
                item.funcaoOperacional().name(),
                item.capacidades().stream().map(Enum::name).toList(),
                item.delegacaoAtiva(),
                item.substituicaoAtiva(),
                item.coberturaAtiva(),
                item.plantaoAtivo(),
                item.totalPendencias(),
                item.totalSemLeitura(),
                item.totalUrgentes(),
                item.totalAtribuidasAoUsuario(),
                item.landingPanel().name(),
                item.landingPath(),
                item.accentColor(),
                item.prioridade(),
                item.fundamentosEntrada());
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> enumType) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT));
    }
}
