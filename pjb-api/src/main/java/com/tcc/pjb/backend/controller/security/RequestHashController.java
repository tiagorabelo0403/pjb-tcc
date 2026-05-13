package com.tcc.pjb.backend.controller.security;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.security.RequestHashComputeRequest;
import com.tcc.pjb.backend.model.dto.security.RequestHashResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/request-hash")
@PreAuthorize("permitAll()")
public class RequestHashController {

    private final SecuritySurfaceFacadeService facadeService;

    public RequestHashController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping
    public ResponseEntity<RequestHashResponse> compute(@Valid @RequestBody RequestHashComputeRequest request) {
        return ResponseEntity.ok(facadeService.computeRequestHash(request));
    }
}
