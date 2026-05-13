package com.tcc.pjb.backend.controller.intelligence;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;
import com.tcc.pjb.backend.service.recursal.RecursalContextualAccessService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;

@RestController
@RequestMapping("/api/v1/intelligence/recursal/mesh")
@Validated
@PreAuthorize("isAuthenticated()")
public class RecursalMeshController {

    private final NationalRecursalMeshService service;
    private final RecursalContextualAccessService accessService;

    public RecursalMeshController(NationalRecursalMeshService service,
                                  RecursalContextualAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
    }

    @PostMapping("/plan")
    public ResponseEntity<RecursalPlanningResult> plan(@RequestBody RecursalMeshPlanRequest request) {
        accessService.requirePlanningAccess(request);
        return ResponseEntity.ok(service.plan(request));
    }

    @PostMapping("/transition")
    public ResponseEntity<RecursalTransitionResult> transition(@RequestBody RecursalMeshTransitionRequest request) {
        accessService.requireTransitionAccess(request);
        return ResponseEntity.ok(service.transition(request));
    }
}
