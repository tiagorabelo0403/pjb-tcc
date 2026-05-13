package com.tcc.pjb.backend.controller.ui;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.ui.WcagAaaAuditRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.ui.surface.UiAccessibilitySurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ui/accessibility/wcag-aaa")
public class WcagAaaAuditController {

    private final UiAccessibilitySurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public WcagAaaAuditController(UiAccessibilitySurfaceFacadeService facadeService,
                                  CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> auditar(@Valid @RequestBody WcagAaaAuditRequest request,
                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "ui_wcg_aaa_audit", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.auditarWcagAaa(request));
    }
}
