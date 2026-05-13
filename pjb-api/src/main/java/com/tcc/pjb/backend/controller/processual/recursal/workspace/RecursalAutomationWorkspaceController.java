package com.tcc.pjb.backend.controller.processual.recursal.workspace;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalAutomationWorkspaceController {

    private final RecursalAutomationWorkspaceService workspaceService;

    public RecursalAutomationWorkspaceController(RecursalAutomationWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping(RecursalRoutes.AUTOMATION_WORKSPACE)
    public ResponseEntity<RecursalAutomationWorkspaceResponse> workspace(@RequestBody RecursalAutomationRequest request) {
        return ResponseEntity.ok(workspaceService.buildWorkspace(request));
    }
}
