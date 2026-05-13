package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorAdminOperationRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.service.integration.judicial.surface.JudicialConnectorOpsSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/ops")
public class AdminJudicialConnectorOpsController {

    private final JudicialConnectorOpsSurfaceFacadeService facadeService;

    public AdminJudicialConnectorOpsController(JudicialConnectorOpsSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> history() {
        return ResponseEntity.ok(facadeService.history());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceActionResponse> execute(@RequestBody JudicialConnectorAdminOperationRequest request) {
        return ResponseEntity.ok(facadeService.execute(request));
    }
}
