package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.affiliation;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationHomologateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.InstitutionalRootAdminApprovalDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalUnitGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalAffiliationController {

    private final NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalAffiliationController(NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAffiliationResponse> solicitarAfiliacao(@RequestBody NationalCommunicationInstitutionalAffiliationCreateRequest request) {
        return ResponseEntity.ok(facadeService.solicitarAfiliacao(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_HOMOLOGATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAffiliationResponse> homologarAfiliacao(@PathVariable String affiliationId,
                                                                                                    @RequestBody NationalCommunicationInstitutionalAffiliationHomologateRequest request) {
        return ResponseEntity.ok(facadeService.homologarAfiliacao(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalAffiliationResponse>> afiliacoes() {
        return ResponseEntity.ok(facadeService.afiliacoes());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_PUBLIC_RECOGNITION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdminInstitutionalPublicRecognitionResponse> reconhecimentoPublicoAfiliacao(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.reconhecimentoPublicoAfiliacao(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_DOSSIER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceDossierResponse> dossieFontesOficiaisAfiliacao(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.dossieFontesOficiaisAfiliacao(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialIdentifierDossierResponse> identificadoresOficiaisAfiliacao(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.identificadoresOficiaisAfiliacao(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceAttestationResponse> atestacaoFontesOficiaisAfiliacao(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.atestacaoFontesOficiaisAfiliacao(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_OFFICIAL_SOURCE_CONNECTORS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse> conectoresFontesOficiais() {
        return ResponseEntity.ok(facadeService.catalogoConectoresFontesOficiais());
    }

    @PostMapping(InstitutionalApiRoutes.PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ALL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse> sondarConectoresFontesOficiais() {
        return ResponseEntity.ok(facadeService.sondarConectoresFontesOficiais());
    }

    @PostMapping(InstitutionalApiRoutes.PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ONE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceConnectorResponse> sondarConectorFonteOficial(@PathVariable String sourceCode) {
        return ResponseEntity.ok(facadeService.sondarConectorFonteOficial(sourceCode));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceAttestationResponse> revalidarFontesOficiaisAfiliacao(@PathVariable String affiliationId,
                                                                                                                                 @RequestBody(required = false) NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return ResponseEntity.ok(facadeService.revalidarFontesOficiaisAfiliacao(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ONBOARDING_PLAN)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOnboardingPlanResponse> planoOnboarding(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.planoOnboarding(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_AUTHENTICATION_POLICY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAuthenticationPolicyResponse> politicaAutenticacao(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.politicaAutenticacao(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_PROVISIONING)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperationalProvisioningResponse> provisionamentoOperacional(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.provisionamentoOperacional(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_PROVISION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperationalProvisioningResponse> provisionarOperacional(@PathVariable String affiliationId,
                                                                                                                    @RequestBody(required = false) NationalCommunicationInstitutionalOperationalProvisioningRequest request) {
        return ResponseEntity.ok(facadeService.provisionarOperacional(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalManagedCredentialResponse>> credenciaisGerenciadas(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.credenciaisGerenciadas(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalManagedCredentialResponse> emitirCredencialGerenciada(@PathVariable String affiliationId,
                                                                                                                  @RequestBody NationalCommunicationInstitutionalManagedCredentialIssueRequest request) {
        return ResponseEntity.ok(facadeService.emitirCredencialGerenciada(affiliationId, request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIAL_REVOKE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalManagedCredentialResponse> revogarCredencialGerenciada(@PathVariable String affiliationId,
                                                                                                                   @PathVariable String credentialId,
                                                                                                                   @RequestBody(required = false) com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return ResponseEntity.ok(facadeService.revogarCredencialGerenciada(affiliationId, credentialId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ROOT_ADMIN_APPROVAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRootAdministratorApprovalResponse> aprovacaoAdministradorRaiz(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.aprovacaoAdministradorRaiz(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ROOT_ADMIN_APPROVAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRootAdministratorApprovalResponse> decidirAprovacaoAdministradorRaiz(@PathVariable String affiliationId,
                                                                                                                                 @RequestBody InstitutionalRootAdminApprovalDecisionRequest request) {
        return ResponseEntity.ok(facadeService.decidirAprovacaoAdministradorRaiz(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_STRONG_SIGNATURE_POLICY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalStrongSignaturePolicyResponse> assinaturaForte(@PathVariable String affiliationId,
                                                                                                           @RequestParam(required = false) String nominationId) {
        return ResponseEntity.ok(facadeService.assinaturaForte(affiliationId, nominationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_UNIT_GOVERNANCE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalUnitGovernanceResponse> governancaUnidades(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.governancaUnidades(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_UNIT_GOVERNANCE_UNITS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalUnitGovernanceResponse> registrarUnidade(@PathVariable String affiliationId,
                                                                                                     @RequestBody NationalCommunicationInstitutionalManagedUnitUpsertRequest request) {
        return ResponseEntity.ok(facadeService.registrarUnidade(affiliationId, request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_UNIT_GOVERNANCE_LOTACOES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalUnitGovernanceResponse> registrarLotacao(@PathVariable String affiliationId,
                                                                                                     @RequestBody NationalCommunicationInstitutionalLotationUpsertRequest request) {
        return ResponseEntity.ok(facadeService.registrarLotacao(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_WORKLOAD_IDENTITY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalWorkloadIdentityPlanResponse> identidadeWorkload(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.identidadeWorkload(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_COVERAGE_DELEGATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalCoverageDelegationResponse> delegacoesCobertura(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.delegacoesCobertura(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_COVERAGE_DELEGATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalCoverageDelegationResponse> registrarDelegacaoCobertura(@PathVariable String affiliationId,
                                                                                                                     @RequestBody NationalCommunicationInstitutionalCoverageDelegationUpsertRequest request) {
        return ResponseEntity.ok(facadeService.registrarDelegacaoCobertura(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_API_EDGE_PROFILE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse> perfilSegurancaApi(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.perfilSegurancaApi(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_NOMINATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalNominationResponse> nomear(@RequestBody NationalCommunicationInstitutionalNominationCreateRequest request) {
        return ResponseEntity.ok(facadeService.nomear(request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_NOMINATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalNominationResponse>> nomeacoes(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(facadeService.nomeacoes(userId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_SECURE_ENTRY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalSecureEntrySummaryResponse> entradaSegura() {
        return ResponseEntity.ok(facadeService.entradaSegura());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_ACCESS_CATALOG)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalAccessProfileCatalogResponse>> catalogoAcessos() {
        return ResponseEntity.ok(facadeService.catalogoAcessos());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_BLUEPRINTS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalOrganizationBlueprintResponse>> blueprints(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(facadeService.blueprints(scope));
    }
}
