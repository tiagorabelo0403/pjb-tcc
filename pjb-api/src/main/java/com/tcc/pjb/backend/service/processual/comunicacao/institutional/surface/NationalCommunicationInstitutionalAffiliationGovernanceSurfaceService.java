package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalDelegatedAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustMatrixApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierCheck;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustMatrixEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalSurfaceFacadeService: fechamento de
 * governança delegada, homologação/listagem de adesão delegada, reconhecimento público e
 * dossiês/atestações de fontes e identificadores oficiais, matriz de confiabilidade -- domínio
 * de governança de afiliação, 6 colaboradores + 6 mapeadores privados usados só por este grupo.
 * `solicitarAdesaoDelegada`/`modeloOperacional`/`entradaInteligente` continuam no facade
 * principal (teste de arquitetura PjbInstitutionalFacadeSpineHardeningTest exige literalmente
 * `stateBundleFacadeService`/`NationalCommunicationInstitutionalFacadeSupport` nesse arquivo).
 */
@Service
public class NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService {

    private final InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService;
    private final InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService;
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService;
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService;
    private final InstitutionalTrustMatrixApplicationService trustMatrixApplicationService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService(
            InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService,
            InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService,
            InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
            InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService,
            InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService,
            InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService,
            InstitutionalTrustMatrixApplicationService trustMatrixApplicationService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.delegatedGovernanceClosureApplicationService = delegatedGovernanceClosureApplicationService;
        this.delegatedAffiliationApplicationService = delegatedAffiliationApplicationService;
        this.publicRecognitionGateApplicationService = publicRecognitionGateApplicationService;
        this.officialSourceDossierApplicationService = officialSourceDossierApplicationService;
        this.officialIdentifierDossierApplicationService = officialIdentifierDossierApplicationService;
        this.officialSourceAttestationApplicationService = officialSourceAttestationApplicationService;
        this.trustMatrixApplicationService = trustMatrixApplicationService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse fechamentoDelegado(String scope) {
        return surfaceAssemblerSupport.toResponse(delegatedGovernanceClosureApplicationService.consolidar(scope));
    }

    public NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse entradaAtualDelegada() {
        return surfaceAssemblerSupport.toResponse(delegatedGovernanceClosureApplicationService.entradaAtual());
    }

    public NationalCommunicationInstitutionalAffiliationRequestResponse homologarAdesaoDelegada(String requestId, NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest request) {
        return surfaceAssemblerSupport.toResponse(delegatedAffiliationApplicationService.homologarSolicitacao(requestId, request.aprovar(), request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalAffiliationRequestResponse> listarAdesoesDelegadas() {
        return delegatedAffiliationApplicationService.listarSolicitacoes().stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAdesaoDelegada(String requestId) {
        return publicRecognitionGateApplicationService.avaliarSolicitacao(requestId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAdesaoDelegada(String requestId) {
        return toOfficialSourceDossierResponse(officialSourceDossierApplicationService.gerarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAdesaoDelegada(String requestId) {
        return toOfficialIdentifierDossierResponse(officialIdentifierDossierApplicationService.gerarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAdesaoDelegada(String requestId) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.consultarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAdesaoDelegada(String requestId,
                                                                                                                       NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.revalidarSolicitacao(
                requestId,
                request == null ? List.of() : request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalTrustMatrixEntryResponse> matrizConfiabilidade(String scope) {
        return trustMatrixApplicationService.listar(scope).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationResponse toOfficialSourceAttestationResponse(InstitutionalOfficialSourceAttestation attestation) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationResponse(
                attestation.subjectType(),
                attestation.subjectId(),
                attestation.affiliationId(),
                attestation.requestId(),
                attestation.organizationScope(),
                attestation.orgaoSigla(),
                attestation.unidadeCodigo(),
                attestation.publicRecognitionStatus(),
                attestation.attestationStatus(),
                attestation.sovereignRecognitionReady(),
                attestation.dueNow(),
                attestation.automaticRefreshEligible(),
                attestation.lastAttestedAt(),
                attestation.nextRefreshAt(),
                attestation.blockingIssues(),
                attestation.sources().stream().map(this::toOfficialSourceAttestationItemResponse).toList(),
                attestation.fundamentos(),
                attestation.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse toOfficialSourceAttestationItemResponse(InstitutionalOfficialSourceAttestationItem item) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse(
                item.sourceCode(),
                item.sourceLabel(),
                item.authority(),
                item.authorityScope(),
                item.accessMode(),
                item.refreshMode(),
                item.directGovernmentSource(),
                item.autoRefreshSupported(),
                item.applicable(),
                item.satisfied(),
                item.mandatoryForAutomaticActivation(),
                item.stale(),
                item.refreshRecommended(),
                item.confidenceScore(),
                item.confidenceBand(),
                item.lastVerifiedAt(),
                item.nextRefreshAt(),
                item.integrityHash(),
                item.connectorStatus(),
                item.connectorEnabled(),
                item.connectorLiveVerificationSupported(),
                item.connectorReferenceUrl(),
                item.connectorCheckedAt(),
                item.connectorNextCheckAt(),
                item.connectorSignals(),
                item.connectorBlockers(),
                item.evidenceSignals(),
                item.pendingIssues(),
                item.safeNextSteps(),
                item.fundamentos()
        );
    }

    private NationalCommunicationInstitutionalOfficialIdentifierDossierResponse toOfficialIdentifierDossierResponse(InstitutionalOfficialIdentifierDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialIdentifierDossierResponse(
                dossier.subjectType(),
                dossier.subjectId(),
                dossier.affiliationId(),
                dossier.requestId(),
                dossier.organizationScope(),
                dossier.orgaoSigla(),
                dossier.unidadeCodigo(),
                dossier.overallStatus(),
                dossier.materialEvidenceReady(),
                dossier.generatedAt(),
                dossier.blockingIssues(),
                dossier.checks().stream().map(this::toOfficialIdentifierCheckResponse).toList(),
                dossier.fundamentos(),
                dossier.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialIdentifierCheckResponse toOfficialIdentifierCheckResponse(InstitutionalOfficialIdentifierCheck check) {
        return new NationalCommunicationInstitutionalOfficialIdentifierCheckResponse(
                check.identifierCode(),
                check.identifierLabel(),
                check.sourceCode(),
                check.value(),
                check.normalizedValue(),
                check.status(),
                check.applicable(),
                check.requiredForRecognition(),
                check.readyForRemoteLookup(),
                check.connectorStatus(),
                check.officialLookupUrl(),
                check.evidenceSignals(),
                check.pendingIssues(),
                check.fundamentos(),
                check.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceDossierResponse toOfficialSourceDossierResponse(InstitutionalOfficialSourceDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialSourceDossierResponse(
                dossier.subjectType(),
                dossier.subjectId(),
                dossier.affiliationId(),
                dossier.requestId(),
                dossier.organizationScope(),
                dossier.orgaoSigla(),
                dossier.unidadeCodigo(),
                dossier.publicRecognitionStatus(),
                dossier.sovereignRecognitionReady(),
                dossier.dueNow(),
                dossier.nextMandatoryReviewAt(),
                dossier.blockingIssues(),
                dossier.sources().stream().map(this::toOfficialSourceEvidenceResponse).toList(),
                dossier.fundamentos(),
                dossier.generatedAt()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceEvidenceResponse toOfficialSourceEvidenceResponse(InstitutionalOfficialSourceEvidence evidence) {
        return new NationalCommunicationInstitutionalOfficialSourceEvidenceResponse(
                evidence.sourceCode(),
                evidence.sourceLabel(),
                evidence.sourceGroup(),
                evidence.applicable(),
                evidence.satisfied(),
                evidence.mandatoryForAutomaticActivation(),
                evidence.stale(),
                evidence.lastEvidenceAt(),
                evidence.nextReviewAt(),
                evidence.evidenceSignals(),
                evidence.pendingIssues(),
                evidence.fundamentos()
        );
    }
}
