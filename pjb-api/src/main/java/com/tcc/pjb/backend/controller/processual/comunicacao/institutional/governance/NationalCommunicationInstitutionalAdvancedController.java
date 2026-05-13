package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveDashboardResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalAdvancedController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalAdvancedController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_CATALOGO_CANONICO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalCanonicalCatalogEntryResponse>> catalogoCanonico() {
        return ResponseEntity.ok(facadeService.catalogoCanonico());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_SLA_PREDITIVO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalSlaPredictiveDashboardResponse> slaPreditivo(@RequestParam(required = false) String uf,
                                                                                                            @RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind) {
        return ResponseEntity.ok(facadeService.slaPreditivo(uf, destinatarioKind));
    }
}
