package com.tcc.pjb.backend.controller.profile;

import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.service.profile.surface.PerfilCapabilitySurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile/capabilities-extension")
@PreAuthorize("isAuthenticated()")
public class PerfilCapabilityMatrixExtensionController {

    private final PerfilCapabilitySurfaceFacadeService facadeService;

    public PerfilCapabilityMatrixExtensionController(PerfilCapabilitySurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    public ResponseEntity<CapabilityExtensionResponse> capacidades(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(facadeService.capacidades(role));
    }
}
