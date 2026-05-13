package com.tcc.pjb.backend.controller.security;

import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.service.security.surface.SecurityContextSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityContextController {

    private final SecurityContextSurfaceFacadeService facadeService;

    public SecurityContextController(SecurityContextSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/context")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SecurityContextResponse> context(HttpServletRequest request) {
        return ResponseEntity.ok(facadeService.context(request));
    }
}
