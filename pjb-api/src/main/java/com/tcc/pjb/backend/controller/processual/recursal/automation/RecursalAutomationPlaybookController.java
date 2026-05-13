package com.tcc.pjb.backend.controller.processual.recursal.automation;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalAutomationPlaybookController {

    private final RecursalAutomationPlaybookService playbookService;

    public RecursalAutomationPlaybookController(RecursalAutomationPlaybookService playbookService) {
        this.playbookService = playbookService;
    }

    @PostMapping(RecursalRoutes.AUTOMATION_PLAYBOOK)
    public ResponseEntity<RecursalAutomationPlaybookResponse> playbook(@RequestBody RecursalAutomationRequest request) {
        return ResponseEntity.ok(playbookService.buildPlaybook(request));
    }
}
