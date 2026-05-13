package com.tcc.pjb.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.service.rito.diagnostics.RitoPackCoverageDto;
import com.tcc.pjb.backend.service.rito.diagnostics.RitoPackDiagnosticsService;

@RestController
@RequestMapping("/api/v1/ritos/pack")
@PreAuthorize("isAuthenticated()")
public class RitoPackController {

    private final RitoPackDiagnosticsService diagnostics;

    public RitoPackController(RitoPackDiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GetMapping("/coverage")
    public RitoPackCoverageDto coverage() {
        return diagnostics.coverage();
    }
}
