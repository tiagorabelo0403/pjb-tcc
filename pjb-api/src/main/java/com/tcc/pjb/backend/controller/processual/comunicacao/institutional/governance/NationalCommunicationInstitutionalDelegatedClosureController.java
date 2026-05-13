package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingModelResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalDelegatedClosureController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalDelegatedClosureController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_CLOSURE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse> fechamentoDelegado(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(facadeService.fechamentoDelegado(scope));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_MODELO_OPERACIONAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperatingModelResponse> modeloOperacional(@RequestParam(required = false) String affiliationId,
                                                                                                       @RequestParam(required = false) String destinatarioKind,
                                                                                                       @RequestParam(required = false) String municipio,
                                                                                                       @RequestParam(required = false) String uf) {
        return ResponseEntity.ok(facadeService.modeloOperacional(affiliationId, destinatarioKind, municipio, uf));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_CURRENT_ENTRY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse> entradaAtual() {
        return ResponseEntity.ok(facadeService.entradaAtualDelegada());
    }
}
