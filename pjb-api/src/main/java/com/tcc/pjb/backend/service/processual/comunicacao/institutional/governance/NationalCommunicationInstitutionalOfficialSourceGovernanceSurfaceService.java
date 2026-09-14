package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorProbeApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierCheck;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * reconhecimento público, dossiês/atestações/conectores de fontes oficiais para governança
 * institucional de afiliação. Inclui os 6 helpers privados de mapeamento manual dos records
 * governance/domain (esses métodos usam construtor posicional com 14-29 campos, não delegam).
 */
@Service
public class NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService {

    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService;
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService;
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService;
    private final InstitutionalOfficialSourceConnectorCatalogApplicationService officialSourceConnectorCatalogApplicationService;
    private final InstitutionalOfficialSourceConnectorProbeApplicationService officialSourceConnectorProbeApplicationService;

    public NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService(
            InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
            InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService,
            InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService,
            InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService,
            InstitutionalOfficialSourceConnectorCatalogApplicationService officialSourceConnectorCatalogApplicationService,
            InstitutionalOfficialSourceConnectorProbeApplicationService officialSourceConnectorProbeApplicationService) {
        this.publicRecognitionGateApplicationService = publicRecognitionGateApplicationService;
        this.officialSourceDossierApplicationService = officialSourceDossierApplicationService;
        this.officialIdentifierDossierApplicationService = officialIdentifierDossierApplicationService;
        this.officialSourceAttestationApplicationService = officialSourceAttestationApplicationService;
        this.officialSourceConnectorCatalogApplicationService = officialSourceConnectorCatalogApplicationService;
        this.officialSourceConnectorProbeApplicationService = officialSourceConnectorProbeApplicationService;
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAfiliacao(String affiliationId) {
        return publicRecognitionGateApplicationService.avaliarAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAfiliacao(String affiliationId) {
        return toOfficialSourceDossierResponse(officialSourceDossierApplicationService.gerarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAfiliacao(String affiliationId) {
        return toOfficialIdentifierDossierResponse(officialIdentifierDossierApplicationService.gerarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAfiliacao(String affiliationId) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.consultarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAfiliacao(String affiliationId,
                                                                                                                  NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.revalidarAfiliacao(
                affiliationId,
                request == null ? List.of() : request.fundamentos()));
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse catalogoConectoresFontesOficiais() {
        return officialSourceConnectorCatalogApplicationService.listar();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse sondarConectoresFontesOficiais() {
        return officialSourceConnectorProbeApplicationService.sondarTodos();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorResponse sondarConectorFonteOficial(String sourceCode) {
        return officialSourceConnectorProbeApplicationService.sondar(sourceCode);
    }

    private NationalCommunicationInstitutionalOfficialIdentifierDossierResponse toOfficialIdentifierDossierResponse(InstitutionalOfficialIdentifierDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialIdentifierDossierResponse(
                dossier.subjectType(), dossier.subjectId(), dossier.affiliationId(), dossier.requestId(),
                dossier.organizationScope(), dossier.orgaoSigla(), dossier.unidadeCodigo(),
                dossier.overallStatus(), dossier.materialEvidenceReady(), dossier.generatedAt(),
                dossier.blockingIssues(),
                dossier.checks().stream().map(this::toOfficialIdentifierCheckResponse).toList(),
                dossier.fundamentos(), dossier.integrityHash());
    }

    private NationalCommunicationInstitutionalOfficialIdentifierCheckResponse toOfficialIdentifierCheckResponse(InstitutionalOfficialIdentifierCheck check) {
        return new NationalCommunicationInstitutionalOfficialIdentifierCheckResponse(
                check.identifierCode(), check.identifierLabel(), check.sourceCode(), check.value(),
                check.normalizedValue(), check.status(), check.applicable(), check.requiredForRecognition(),
                check.readyForRemoteLookup(), check.connectorStatus(), check.officialLookupUrl(),
                check.evidenceSignals(), check.pendingIssues(), check.fundamentos(), check.integrityHash());
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationResponse toOfficialSourceAttestationResponse(InstitutionalOfficialSourceAttestation attestation) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationResponse(
                attestation.subjectType(), attestation.subjectId(), attestation.affiliationId(),
                attestation.requestId(), attestation.organizationScope(), attestation.orgaoSigla(),
                attestation.unidadeCodigo(), attestation.publicRecognitionStatus(),
                attestation.attestationStatus(), attestation.sovereignRecognitionReady(),
                attestation.dueNow(), attestation.automaticRefreshEligible(), attestation.lastAttestedAt(),
                attestation.nextRefreshAt(), attestation.blockingIssues(),
                attestation.sources().stream().map(this::toOfficialSourceAttestationItemResponse).toList(),
                attestation.fundamentos(), attestation.integrityHash());
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse toOfficialSourceAttestationItemResponse(InstitutionalOfficialSourceAttestationItem item) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse(
                item.sourceCode(), item.sourceLabel(), item.authority(), item.authorityScope(),
                item.accessMode(), item.refreshMode(), item.directGovernmentSource(),
                item.autoRefreshSupported(), item.applicable(), item.satisfied(),
                item.mandatoryForAutomaticActivation(), item.stale(), item.refreshRecommended(),
                item.confidenceScore(), item.confidenceBand(), item.lastVerifiedAt(),
                item.nextRefreshAt(), item.integrityHash(), item.connectorStatus(),
                item.connectorEnabled(), item.connectorLiveVerificationSupported(),
                item.connectorReferenceUrl(), item.connectorCheckedAt(), item.connectorNextCheckAt(),
                item.connectorSignals(), item.connectorBlockers(), item.evidenceSignals(),
                item.pendingIssues(), item.safeNextSteps(), item.fundamentos());
    }

    private NationalCommunicationInstitutionalOfficialSourceDossierResponse toOfficialSourceDossierResponse(InstitutionalOfficialSourceDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialSourceDossierResponse(
                dossier.subjectType(), dossier.subjectId(), dossier.affiliationId(), dossier.requestId(),
                dossier.organizationScope(), dossier.orgaoSigla(), dossier.unidadeCodigo(),
                dossier.publicRecognitionStatus(), dossier.sovereignRecognitionReady(),
                dossier.dueNow(), dossier.nextMandatoryReviewAt(), dossier.blockingIssues(),
                dossier.sources().stream().map(this::toOfficialSourceEvidenceResponse).toList(),
                dossier.fundamentos(), dossier.generatedAt());
    }

    private NationalCommunicationInstitutionalOfficialSourceEvidenceResponse toOfficialSourceEvidenceResponse(InstitutionalOfficialSourceEvidence evidence) {
        return new NationalCommunicationInstitutionalOfficialSourceEvidenceResponse(
                evidence.sourceCode(), evidence.sourceLabel(), evidence.sourceGroup(),
                evidence.applicable(), evidence.satisfied(), evidence.mandatoryForAutomaticActivation(),
                evidence.stale(), evidence.lastEvidenceAt(), evidence.nextReviewAt(),
                evidence.evidenceSignals(), evidence.pendingIssues(), evidence.fundamentos());
    }
}
