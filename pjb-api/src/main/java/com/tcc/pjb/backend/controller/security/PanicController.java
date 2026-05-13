package com.tcc.pjb.backend.controller.security;

import com.tcc.pjb.backend.model.dto.security.PanicStatusResponse;
import com.tcc.pjb.backend.model.dto.security.PanicTriggerRequest;
import com.tcc.pjb.backend.model.dto.security.PanicTriggerResponse;
import com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/security/panic")
public class PanicController {

    private final SecuritySurfaceFacadeService facadeService;

    public PanicController(SecuritySurfaceFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @GetMapping("/status")
    public ResponseEntity<PanicStatusResponse> status() {
        return ResponseEntity.ok(facadeService.panicStatus());
    }

    @PostMapping("/trigger")
    public ResponseEntity<PanicTriggerResponse> trigger(@Valid @RequestBody PanicTriggerRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(facadeService.panicTrigger(request, httpRequest));
    }
}
