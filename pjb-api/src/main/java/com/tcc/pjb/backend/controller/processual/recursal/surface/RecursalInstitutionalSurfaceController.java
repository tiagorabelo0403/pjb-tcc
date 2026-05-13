package com.tcc.pjb.backend.controller.processual.recursal.surface;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalInstitutionalSurfaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalInstitutionalSurfaceController {

    private final RecursalInstitutionalSurfaceService institutionalSurfaceService;

    public RecursalInstitutionalSurfaceController(RecursalInstitutionalSurfaceService institutionalSurfaceService) {
        this.institutionalSurfaceService = institutionalSurfaceService;
    }

    @PostMapping(RecursalRoutes.SURFACES_INSTITUTIONAL)
    public ResponseEntity<RecursalSpecializedSurfaceResponse> institutional(@RequestBody RecursalAutomationRequest request) {
        return ResponseEntity.ok(institutionalSurfaceService.buildInstitutionalSurface(request));
    }
}
