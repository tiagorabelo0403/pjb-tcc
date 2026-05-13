package com.tcc.pjb.backend.controller.processual.recursal.documental;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentAuthenticityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentSignatureEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentViewerResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentalArtifactRequest;
import com.tcc.pjb.backend.service.processual.recursal.documental.RecursalDocumentalSovereignSuiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalDocumentalSovereignSuiteController {

    private final RecursalDocumentalSovereignSuiteService sovereignSuiteService;

    public RecursalDocumentalSovereignSuiteController(RecursalDocumentalSovereignSuiteService sovereignSuiteService) {
        this.sovereignSuiteService = sovereignSuiteService;
    }

    @PostMapping(RecursalRoutes.DOCUMENT_VIEWER)
    public ResponseEntity<RecursalDocumentViewerResponse> viewer(@RequestBody RecursalDocumentalArtifactRequest request) {
        return ResponseEntity.ok(sovereignSuiteService.viewer(request));
    }

    @PostMapping(RecursalRoutes.DOCUMENT_AUTHENTICITY)
    public ResponseEntity<RecursalDocumentAuthenticityResponse> authenticity(@RequestBody RecursalDocumentalArtifactRequest request) {
        return ResponseEntity.ok(sovereignSuiteService.authenticity(request));
    }

    @PostMapping(RecursalRoutes.DOCUMENT_SIGNATURE_EVIDENCE)
    public ResponseEntity<RecursalDocumentSignatureEvidenceResponse> signature(@RequestBody RecursalDocumentalArtifactRequest request) {
        return ResponseEntity.ok(sovereignSuiteService.signature(request));
    }
}
