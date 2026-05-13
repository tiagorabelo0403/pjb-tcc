package com.tcc.pjb.backend.controller.processual.recursal.surface;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalIntelligenceSurfaceController {

    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService;

    public RecursalIntelligenceSurfaceController(RecursalIntelligenceSurfaceService intelligenceSurfaceService) {
        this.intelligenceSurfaceService = intelligenceSurfaceService;
    }

    @PostMapping(RecursalRoutes.SURFACES_INTELLIGENCE)
    public ResponseEntity<RecursalSpecializedSurfaceResponse> intelligence(@RequestBody RecursalAutomationRequest request) {
        return ResponseEntity.ok(intelligenceSurfaceService.buildIntelligenceSurface(request));
    }
}
