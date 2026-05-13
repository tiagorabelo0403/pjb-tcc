package com.tcc.pjb.backend.controller.security;

import com.tcc.pjb.backend.model.dto.security.govbr.GovBrIdentityAssuranceResponse;
import com.tcc.pjb.backend.service.security.govbr.GovBrSurfaceFacadeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/auth/govbr")
public class GovBrIdentityAssuranceController {

    private final GovBrSurfaceFacadeService facadeService;

    public GovBrIdentityAssuranceController(GovBrSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(value = "/identity-assurance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GovBrIdentityAssuranceResponse> identityAssurance() {
        return ResponseEntity.ok(facadeService.identityAssurance());
    }
}
