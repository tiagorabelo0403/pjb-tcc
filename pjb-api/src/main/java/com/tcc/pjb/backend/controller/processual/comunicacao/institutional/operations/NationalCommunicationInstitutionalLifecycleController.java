package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.operations;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalLifecycleResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalLifecycleController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalLifecycleController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CADASTROS_OPERACIONAIS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalOperationalLifecycleResponse>> cadastrosOperacionais() {
        return ResponseEntity.ok(facadeService.cadastrosOperacionais());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CADASTRO_OPERACIONAL_AFILIACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharAfiliacao(@PathVariable String affiliationId) {
        return facadeService.detalharAfiliacaoLifecycle(affiliationId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CADASTRO_OPERACIONAL_SOLICITACAO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharSolicitacao(@PathVariable String requestId) {
        return facadeService.detalharSolicitacaoLifecycle(requestId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_GUARDIAO_ENTRADA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalEntryGuardResponse> guardiaoEntrada() {
        return ResponseEntity.ok(facadeService.guardiaoEntrada());
    }
}
