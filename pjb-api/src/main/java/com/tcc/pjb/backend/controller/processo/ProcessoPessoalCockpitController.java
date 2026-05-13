package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalCockpitResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaPersonalCockpitService;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processos/pessoais")
public class ProcessoPessoalCockpitController {

    private final ConsultaPublicaPersonalCockpitService consultaPublicaPersonalCockpitService;
    private final CapabilityRateLimiter rateLimiter;

    public ProcessoPessoalCockpitController(ConsultaPublicaPersonalCockpitService consultaPublicaPersonalCockpitService,
                                            CapabilityRateLimiter rateLimiter) {
        this.consultaPublicaPersonalCockpitService = consultaPublicaPersonalCockpitService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/cockpit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConsultaPublicaPersonalCockpitResponse> cockpit(
            Authentication authentication,
            @RequestParam(value = "processoId", required = false) Long processoId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "personal_process_cockpit", ApiVersion.V1);
        ConsultaPublicaPersonalCockpitResponse payload = consultaPublicaPersonalCockpitService.cockpit(authentication, processoId, from, to);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePrivate().mustRevalidate())
                .body(payload);
    }
}
