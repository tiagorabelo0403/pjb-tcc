package com.tcc.pjb.backend.controller.security;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.security.BaptismCompleteRequest;
import com.tcc.pjb.backend.model.dto.security.BaptismStartResponse;
import com.tcc.pjb.backend.model.dto.security.SecurityOperationResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/baptism")
@PreAuthorize("permitAll()")
public class AdvogadoBaptismController {

    private final SecuritySurfaceFacadeService facadeService;

    public AdvogadoBaptismController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/start")
    public ResponseEntity<BaptismStartResponse> start(HttpServletRequest request) {
        return ResponseEntity.ok(facadeService.startBaptism(request));
    }

    @PostMapping("/complete")
    public ResponseEntity<SecurityOperationResponse> complete(@Valid @RequestBody BaptismCompleteRequest request) {
        return ResponseEntity.ok(facadeService.completeBaptism(request));
    }
}
