package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.operations;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageApplyRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalOperationsController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalOperationsController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping(InstitutionalApiRoutes.PATH_RECEBER_LOTE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalBulkResponse> receberLote(@Valid @RequestBody NationalCommunicationInstitutionalBulkRequest request) {
        return ResponseEntity.ok(facadeService.receberLote(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_CERTIFICAR_CIENCIA_LOTE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalBulkResponse> certificarCienciaLote(@Valid @RequestBody NationalCommunicationInstitutionalBulkRequest request) {
        return ResponseEntity.ok(facadeService.certificarCienciaLote(request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_TRIAGEM_SUGERIDA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalTriageSuggestionDashboardResponse> triagemSugerida(@RequestParam String expedicaoUuid) {
        return ResponseEntity.ok(facadeService.triagemSugerida(expedicaoUuid));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CONTRATO_INTEGRACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalIntegrationContractDescriptorResponse>> contratoIntegracao() {
        return ResponseEntity.ok(facadeService.contratoIntegracao());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_COBERTURAS_OPERACIONAIS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalCoverageResponse>> coberturasOperacionais(@RequestParam(required = false) String unidadeCodigo) {
        return ResponseEntity.ok(facadeService.coberturasOperacionais(unidadeCodigo));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_COBERTURAS_OPERACIONAIS_APLICAR)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalDelegationResponse>> aplicarCoberturasAtivas(@Valid @RequestBody NationalCommunicationInstitutionalCoverageApplyRequest request) {
        return ResponseEntity.ok(facadeService.aplicarCoberturasAtivas(request));
    }
}