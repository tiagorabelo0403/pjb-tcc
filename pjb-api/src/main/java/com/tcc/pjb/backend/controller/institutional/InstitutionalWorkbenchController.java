package com.tcc.pjb.backend.controller.institutional;

import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionPreviewResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWorkspaceResponse;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchProjectionService;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institucional/workbench")
@PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
public class InstitutionalWorkbenchController {

    private final InstitutionalWorkbenchService institutionalWorkbenchService;
    private final InstitutionalWorkbenchProjectionService institutionalWorkbenchProjectionService;

    public InstitutionalWorkbenchController(InstitutionalWorkbenchService institutionalWorkbenchService,
                                            InstitutionalWorkbenchProjectionService institutionalWorkbenchProjectionService) {
        this.institutionalWorkbenchService = institutionalWorkbenchService;
        this.institutionalWorkbenchProjectionService = institutionalWorkbenchProjectionService;
    }

    @GetMapping
    public ResponseEntity<InstitutionalWorkbenchWorkspaceResponse> workspace() {
        return ResponseEntity.ok(institutionalWorkbenchService.workspace());
    }

    @GetMapping("/quick-actions")
    public ResponseEntity<InstitutionalWorkbenchQuickActionsResponse> quickActions(@RequestParam(value = "processoId", required = false) Long processoId) {
        return ResponseEntity.ok(institutionalWorkbenchProjectionService.quickActions(processoId));
    }

    @GetMapping("/operational-queue")
    public ResponseEntity<InstitutionalWorkbenchOperationalQueueResponse> operationalQueue(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(institutionalWorkbenchProjectionService.operationalQueue(limit));
    }

    @GetMapping("/action-preview")
    public ResponseEntity<InstitutionalWorkbenchActionPreviewResponse> actionPreview(@RequestParam("action") String action,
                                                                                     @RequestParam(value = "processoId", required = false) Long processoId) {
        return ResponseEntity.ok(institutionalWorkbenchService.actionPreview(processoId, action));
    }
}
