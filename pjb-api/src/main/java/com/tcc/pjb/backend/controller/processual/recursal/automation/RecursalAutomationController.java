package com.tcc.pjb.backend.controller.processual.recursal.automation;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalAutomationController {

    private final RecursalAutomationService automationService;

    public RecursalAutomationController(RecursalAutomationService automationService) {
        this.automationService = automationService;
    }

    @PostMapping(RecursalRoutes.AUTOMATION_ADVISE)
    public ResponseEntity<RecursalAutomationResponse> advise(@RequestBody RecursalAutomationRequest request) {
        return ResponseEntity.ok(automationService.advise(request));
    }
}
