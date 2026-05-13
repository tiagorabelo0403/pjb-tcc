package com.tcc.pjb.backend.modules.atendimento.controller;

import com.tcc.pjb.backend.model.dto.atendimento.AtendimentoTosAcceptRequest;
import com.tcc.pjb.backend.model.dto.atendimento.AtendimentoTosInfoResponse;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoTosService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimento/tos")
public class AtendimentoTosController {

    private final AtendimentoTosService service;
    private final CapabilityRateLimiter rateLimiter;

    public AtendimentoTosController(AtendimentoTosService service, CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<AtendimentoTosInfoResponse> info(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_tos_info", ApiVersion.V1);
        var info = service.info();
        return ResponseEntity.ok(new AtendimentoTosInfoResponse(info.requiredVersion(), info.tosUrl(), info.accepted(), info.acceptedVersion()));
    }

    @PostMapping("/accept")
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<Void> accept(Authentication authentication, @RequestBody AtendimentoTosAcceptRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_tos_accept", ApiVersion.V1);
        service.accept(request.version());
        return ResponseEntity.ok().build();
    }
}
