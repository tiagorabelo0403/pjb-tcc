package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.affiliation;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustMatrixEntryResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
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
public class NationalCommunicationInstitutionalDelegatedOnboardingController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalDelegatedOnboardingController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAffiliationRequestResponse> solicitarAdesaoDelegada(@RequestBody NationalCommunicationInstitutionalDelegatedAffiliationCreateRequest request) {
        return ResponseEntity.ok(facadeService.solicitarAdesaoDelegada(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_HOMOLOGATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAffiliationRequestResponse> homologarAdesaoDelegada(@PathVariable String requestId,
                                                                                                                 @RequestBody NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest request) {
        return ResponseEntity.ok(facadeService.homologarAdesaoDelegada(requestId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalAffiliationRequestResponse>> listarAdesoesDelegadas() {
        return ResponseEntity.ok(facadeService.listarAdesoesDelegadas());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_PUBLIC_RECOGNITION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdminInstitutionalPublicRecognitionResponse> reconhecimentoPublicoAdesaoDelegada(@PathVariable String requestId) {
        return ResponseEntity.ok(facadeService.reconhecimentoPublicoAdesaoDelegada(requestId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceDossierResponse> dossieFontesOficiaisAdesaoDelegada(@PathVariable String requestId) {
        return ResponseEntity.ok(facadeService.dossieFontesOficiaisAdesaoDelegada(requestId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialIdentifierDossierResponse> identificadoresOficiaisAdesaoDelegada(@PathVariable String requestId) {
        return ResponseEntity.ok(facadeService.identificadoresOficiaisAdesaoDelegada(requestId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceAttestationResponse> atestacaoFontesOficiaisAdesaoDelegada(@PathVariable String requestId) {
        return ResponseEntity.ok(facadeService.atestacaoFontesOficiaisAdesaoDelegada(requestId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOfficialSourceAttestationResponse> revalidarFontesOficiaisAdesaoDelegada(@PathVariable String requestId,
                                                                                                                                    @RequestBody(required = false) NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return ResponseEntity.ok(facadeService.revalidarFontesOficiaisAdesaoDelegada(requestId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_TRUST_MATRIX)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalTrustMatrixEntryResponse>> matrizConfiabilidade(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(facadeService.matrizConfiabilidade(scope));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PANEL_BLUEPRINTS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalPanelBlueprintResponse>> painelBlueprints(@RequestParam(required = false) String scope,
                                                                                                            @RequestParam(required = false) String panel) {
        return ResponseEntity.ok(facadeService.painelBlueprints(scope, panel));
    }
}
