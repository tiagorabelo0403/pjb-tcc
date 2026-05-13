package com.tcc.pjb.backend.controller.security;

import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.model.dto.security.BodyHashResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/body-hash")
@PreAuthorize("permitAll()")
public class BodyHashController {

    private final SecuritySurfaceFacadeService facadeService;

    public BodyHashController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping
    public ResponseEntity<BodyHashResponse> compute(@RequestBody JsonNode body) {
        return ResponseEntity.ok(facadeService.computeBodyHash(body));
    }
}
