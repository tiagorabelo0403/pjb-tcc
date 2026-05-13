package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeRitosCoverageResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeRitosCoverageService;

@RestController
@RequestMapping("/api/v1/laiane/ritos")
@PreAuthorize("isAuthenticated()")
public class LaianeRitosController {

    private final LaianeRitosCoverageService coverageService;

    public LaianeRitosController(LaianeRitosCoverageService coverageService) {
        this.coverageService = coverageService;
    }

    @GetMapping("/coverage")
    public LaianeRitosCoverageResponse coverage() {
        return coverageService.coverage();
    }
}
