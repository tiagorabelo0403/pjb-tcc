package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalSecureEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationHomologateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.InstitutionalRootAdminApprovalDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalUnitGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.InstitutionalRemoteCertificateAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalGovernanceSurfaceFacadeService {

    private final InstitutionalAffiliationApplicationService affiliationService;
    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;
    private final NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService officialSourceGovernance;
    private final NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService accessLaneGovernance;
    private final NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService credentialsGovernance;
    private final NationalCommunicationInstitutionalUnitGovernanceSurfaceService unitGovernance;
    private final NationalCommunicationInstitutionalAccessSecuritySurfaceService accessSecurity;
    private final NationalCommunicationInstitutionalAuthorizationTrailSurfaceService authorizationTrail;
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalGovernanceSurfaceFacadeService(
            InstitutionalAffiliationApplicationService affiliationService,
            InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService,
            NationalCommunicationInstitutionalOfficialSourceGovernanceSurfaceService officialSourceGovernance,
            NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService accessLaneGovernance,
            NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService credentialsGovernance,
            NationalCommunicationInstitutionalUnitGovernanceSurfaceService unitGovernance,
            NationalCommunicationInstitutionalAccessSecuritySurfaceService accessSecurity,
            NationalCommunicationInstitutionalAuthorizationTrailSurfaceService authorizationTrail,
            NationalCommunicationInstitutionalFacadeSupport facadeSupport,
            NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.affiliationService = affiliationService;
        this.trustGovernanceOrchestrationApplicationService = trustGovernanceOrchestrationApplicationService;
        this.officialSourceGovernance = officialSourceGovernance;
        this.accessLaneGovernance = accessLaneGovernance;
        this.credentialsGovernance = credentialsGovernance;
        this.unitGovernance = unitGovernance;
        this.accessSecurity = accessSecurity;
        this.authorizationTrail = authorizationTrail;
        this.facadeSupport = facadeSupport;
        this.stateBundleFacadeService = stateBundleFacadeService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalAffiliationResponse solicitarAfiliacao(NationalCommunicationInstitutionalAffiliationCreateRequest request) {
        return toAffiliation(affiliationService.solicitarAfiliacao(
                facadeSupport.parseDestinatarioKind(request.destinatarioInstitucionalKind()),
                facadeSupport.parseOrganizationScope(request.organizationScope()),
                request.orgaoSigla(),
                request.orgaoNome(),
                request.unidadeCodigo(),
                request.unidadeNome(),
                request.uf(),
                request.comarca(),
                request.cnpj(),
                request.esferaAdministrativa(),
                request.ramosMateriais(),
                request.abrangenciasTerritoriais(),
                request.dominioInstitucional(),
                request.autoridadeAderenteCargo(),
                request.emailContatoSeguranca(),
                facadeSupport.parseNominationRole(request.representativeRole()),
                facadeSupport.parseTrustLevel(request.trustFloor()),
                request.requerDuplaAprovacaoAdministrador(),
                request.requerCertificadoICP(),
                request.restringeCertificadoRedeInstitucional(),
                request.permiteUsoRemotoComAutorizacao(),
                request.canaisHabilitados(),
                request.politicaCiencia(),
                request.sla(),
                request.regrasFallback(),
                request.conveniosIntegracoes(),
                request.fundamentos()));
    }

    public NationalCommunicationInstitutionalAffiliationResponse homologarAfiliacao(String affiliationId, NationalCommunicationInstitutionalAffiliationHomologateRequest request) {
        return toAffiliation(affiliationService.homologarAfiliacao(affiliationId, request.homologar(), request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalAffiliationResponse> afiliacoes() {
        return affiliationService.listarAfiliacoes().stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAfiliacao(String affiliationId) {
        return officialSourceGovernance.reconhecimentoPublicoAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAfiliacao(String affiliationId) {
        return officialSourceGovernance.dossieFontesOficiaisAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAfiliacao(String affiliationId) {
        return officialSourceGovernance.identificadoresOficiaisAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAfiliacao(String affiliationId) {
        return officialSourceGovernance.atestacaoFontesOficiaisAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAfiliacao(String affiliationId,
                                                                                                                  NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return officialSourceGovernance.revalidarFontesOficiaisAfiliacao(affiliationId, request);
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse catalogoConectoresFontesOficiais() {
        return officialSourceGovernance.catalogoConectoresFontesOficiais();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse sondarConectoresFontesOficiais() {
        return officialSourceGovernance.sondarConectoresFontesOficiais();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorResponse sondarConectorFonteOficial(String sourceCode) {
        return officialSourceGovernance.sondarConectorFonteOficial(sourceCode);
    }

    public NationalCommunicationInstitutionalNominationResponse nomear(NationalCommunicationInstitutionalNominationCreateRequest request) {
        return toNomination(affiliationService.nomearPessoa(
                request.affiliationId(),
                request.nominatedUserId(),
                request.nominatedUserName(),
                request.tipoUsuario() == null ? null : TipoUsuario.fromPerfil(request.tipoUsuario()),
                facadeSupport.parseAccessLaneKind(request.accessLaneKind()),
                facadeSupport.parseNominationRole(request.nominationRole()),
                facadeSupport.parseFuncaoOperacional(request.funcaoOperacional()),
                facadeSupport.parseProcessProfile(request.processProfile()),
                request.unidadeCodigo(),
                request.caixaCodigo(),
                facadeSupport.parseCapacidades(request.capacidades()),
                facadeSupport.parseTrustLevel(request.trustFloor()),
                facadeSupport.parseLandingPanel(request.panelPreferencial()),
                request.ativaDe(),
                request.ativaAte(),
                request.requerStepUpMfa(),
                request.requerCertificadoICP(),
                request.requerRedeInstitucional(),
                request.permiteUsoRemotoAutorizado()));
    }

    public List<NationalCommunicationInstitutionalNominationResponse> nomeacoes(Long userId) {
        return affiliationService.listarNomeacoes(userId).stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalSecureEntrySummaryResponse entradaSegura() {
        InstitutionalSecureEntrySummary summary = affiliationService.avaliarEntradaSeguraAtual();
        return governanceAssemblerSupport.toResponse(summary);
    }

    public List<NationalCommunicationInstitutionalAccessProfileCatalogResponse> catalogoAcessos() {
        return accessLaneGovernance.catalogoAcessos();
    }

    public List<NationalCommunicationInstitutionalOrganizationBlueprintResponse> blueprints(String scope) {
        return accessLaneGovernance.blueprints(scope);
    }

    public NationalCommunicationInstitutionalOnboardingPlanResponse planoOnboarding(String affiliationId) {
        return accessLaneGovernance.planoOnboarding(affiliationId);
    }

    public NationalCommunicationInstitutionalAuthenticationPolicyResponse politicaAutenticacao(String affiliationId) {
        return accessLaneGovernance.politicaAutenticacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOperationalProvisioningResponse provisionamentoOperacional(String affiliationId) {
        return accessLaneGovernance.provisionamentoOperacional(affiliationId);
    }

    public NationalCommunicationInstitutionalOperationalProvisioningResponse provisionarOperacional(String affiliationId,
                                                                                                    NationalCommunicationInstitutionalOperationalProvisioningRequest request) {
        return accessLaneGovernance.provisionarOperacional(affiliationId, request);
    }

    public List<NationalCommunicationInstitutionalManagedCredentialResponse> credenciaisGerenciadas(String affiliationId) {
        return credentialsGovernance.credenciaisGerenciadas(affiliationId);
    }

    public NationalCommunicationInstitutionalManagedCredentialResponse emitirCredencialGerenciada(String affiliationId,
                                                                                                  NationalCommunicationInstitutionalManagedCredentialIssueRequest request) {
        return credentialsGovernance.emitirCredencialGerenciada(affiliationId, request);
    }

    public NationalCommunicationInstitutionalManagedCredentialResponse revogarCredencialGerenciada(String affiliationId,
                                                                                                   String credentialId,
                                                                                                   NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return credentialsGovernance.revogarCredencialGerenciada(affiliationId, credentialId, request);
    }

    public NationalCommunicationInstitutionalRootAdministratorApprovalResponse aprovacaoAdministradorRaiz(String affiliationId) {
        return credentialsGovernance.aprovacaoAdministradorRaiz(affiliationId);
    }

    public NationalCommunicationInstitutionalRootAdministratorApprovalResponse decidirAprovacaoAdministradorRaiz(String affiliationId,
                                                                                                                InstitutionalRootAdminApprovalDecisionRequest request) {
        return credentialsGovernance.decidirAprovacaoAdministradorRaiz(affiliationId, request);
    }

    public NationalCommunicationInstitutionalStrongSignaturePolicyResponse assinaturaForte(String affiliationId, String nominationId) {
        return credentialsGovernance.assinaturaForte(affiliationId, nominationId);
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse governancaUnidades(String affiliationId) {
        return unitGovernance.governancaUnidades(affiliationId);
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarUnidade(String affiliationId,
                                                                                      NationalCommunicationInstitutionalManagedUnitUpsertRequest request) {
        return unitGovernance.registrarUnidade(affiliationId, request);
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarLotacao(String affiliationId,
                                                                                      NationalCommunicationInstitutionalLotationUpsertRequest request) {
        return unitGovernance.registrarLotacao(affiliationId, request);
    }

    public NationalCommunicationInstitutionalWorkloadIdentityPlanResponse identidadeWorkload(String affiliationId) {
        return unitGovernance.identidadeWorkload(affiliationId);
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse delegacoesCobertura(String affiliationId) {
        return unitGovernance.delegacoesCobertura(affiliationId);
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse registrarDelegacaoCobertura(String affiliationId,
                                                                                                    NationalCommunicationInstitutionalCoverageDelegationUpsertRequest request) {
        return unitGovernance.registrarDelegacaoCobertura(affiliationId, request);
    }

    public NationalCommunicationInstitutionalAccessContextResponse contextoAcesso(String affiliationId, String nominationId) {
        return accessSecurity.contextoAcesso(affiliationId, nominationId);
    }

    public NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse perfilSegurancaApi(String affiliationId) {
        return accessSecurity.perfilSegurancaApi(affiliationId);
    }

    public List<NationalCommunicationInstitutionalRecertificationResponse> recertificacoes(String scope) {
        return accessSecurity.recertificacoes(scope);
    }

    public NationalCommunicationInstitutionalRecertificationResponse recertificar(String affiliationId, NationalCommunicationInstitutionalRecertificationRequest request) {
        return accessSecurity.recertificar(affiliationId, request);
    }

    public NationalCommunicationInstitutionalRevocationResponse revogarAcessos(String affiliationId, NationalCommunicationInstitutionalRevocationRequest request) {
        return accessSecurity.revogarAcessos(affiliationId, request);
    }

    public List<NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse> integracoesGovernanca(String scope, String affiliationId) {
        return accessSecurity.integracoesGovernanca(scope, affiliationId);
    }

    public NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse dimensionamentoUsuariosInternos() {
        return governanceAssemblerSupport.toResponse(trustGovernanceOrchestrationApplicationService.dimensionarUsuariosInternos());
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse governancaConfianca(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.carregar(affiliationId, nominationId).trustGovernanceProfile());
    }

    public NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse planoDadosHorizontal(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.carregar(affiliationId, nominationId).horizontalDataPlanePlan());
    }

    public NationalCommunicationInstitutionalOperationalProfileResponse perfilOperacional(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.materializarPerfil(affiliationId, nominationId));
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse decidirGovernancaConfianca(String affiliationId,
                                                                                                        String nominationId,
                                                                                                        NationalCommunicationInstitutionalTrustGovernanceDecisionRequest request) {
        com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind approvalKind =
                com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind.fromTexto(request == null ? null : request.approvalKind());
        if (approvalKind == null) {
            throw new IllegalArgumentException("Tipo de aprovação institucional não reconhecido.");
        }
        return governanceAssemblerSupport.toResponse(trustGovernanceOrchestrationApplicationService.decidir(
                affiliationId,
                nominationId,
                approvalKind,
                request.approved(),
                request.fundamentos()));
    }

    public Optional<NationalCommunicationInstitutionalAffiliationValidationReportResponse> validacaoAdesao(String requestId) {
        return authorizationTrail.validacaoAdesao(requestId);
    }

    public Optional<NationalCommunicationInstitutionalApprovalTrailResponse> trilhaAprovacao(String requestId) {
        return authorizationTrail.trilhaAprovacao(requestId);
    }

    public NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse emitirAutorizacaoRemota(InstitutionalRemoteCertificateAuthorizationRequest request) {
        return authorizationTrail.emitirAutorizacaoRemota(request);
    }

    public NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse revogarAutorizacaoRemota(String authorizationId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return authorizationTrail.revogarAutorizacaoRemota(authorizationId, request);
    }

    public List<NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse> listarAutorizacoesRemotas(String affiliationId, Long userId) {
        return authorizationTrail.listarAutorizacoesRemotas(affiliationId, userId);
    }

    public NationalCommunicationInstitutionalSessionRiskAssessmentResponse riscoSessao(String affiliationId, String nominationId, String unidadeCodigo, String caixaCodigo) {
        return authorizationTrail.riscoSessao(affiliationId, nominationId, unidadeCodigo, caixaCodigo);
    }

    public NationalCommunicationInstitutionalSensitiveActAuthorizationResponse autorizarAtoSensivel(NationalCommunicationInstitutionalSensitiveActAuthorizationRequest request) {
        return authorizationTrail.autorizarAtoSensivel(request);
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse emitirCredencial(NationalCommunicationInstitutionalIntegrationCredentialIssueRequest request) {
        return credentialsGovernance.emitirCredencial(request);
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse rotacionarCredencial(String credentialId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return credentialsGovernance.rotacionarCredencial(credentialId, request);
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse revogarCredencial(String credentialId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return credentialsGovernance.revogarCredencial(credentialId, request);
    }

    public List<NationalCommunicationInstitutionalIntegrationCredentialResponse> listarCredenciais(String affiliationId) {
        return credentialsGovernance.listarCredenciais(affiliationId);
    }

    public NationalCommunicationInstitutionalIntegrationCallTrailResponse registrarChamada(String credentialId, NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest request) {
        return credentialsGovernance.registrarChamada(credentialId, request);
    }

    public List<NationalCommunicationInstitutionalIntegrationCallTrailResponse> trilhaChamadas(String credentialId) {
        return credentialsGovernance.trilhaChamadas(credentialId);
    }

    private NationalCommunicationInstitutionalAffiliationResponse toAffiliation(InstitutionalAffiliation item) {
        return governanceAssemblerSupport.toResponse(item);
    }

    private NationalCommunicationInstitutionalNominationResponse toNomination(InstitutionalNomination item) {
        return governanceAssemblerSupport.toResponse(item);
    }
}
