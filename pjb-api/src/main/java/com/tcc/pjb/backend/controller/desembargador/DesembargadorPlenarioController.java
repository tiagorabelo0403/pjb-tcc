package com.tcc.pjb.backend.controller.desembargador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.desembargador.DesembargadorPlenarioVotoRequest;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioResponse;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioVoteDto;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.desembargador.DesembargadorPlenarioService;

@RestController
@RequestMapping("/api/v1/desembargador/plenario")
@Validated
@PreAuthorize("hasAnyRole('DESEMBARGADOR','DESEMBARGADOR_FEDERAL')")
public class DesembargadorPlenarioController {

    private final DesembargadorPlenarioService service;
    private final CapabilityRateLimiter rateLimiter;

    public DesembargadorPlenarioController(DesembargadorPlenarioService service,
                                           CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/sessoes/{sessaoId}/relator")
    public ResponseEntity<RelatorPlenarioResponse> painelRelator(@PathVariable Long sessaoId,
                                                                 Authentication authentication) {
        enforce(authentication, "desembargador_plenario_relator");
        return ResponseEntity.ok(service.painelRelator(sessaoId));
    }

    @PostMapping("/sessoes/{sessaoId}/votos")
    public ResponseEntity<RelatorPlenarioVoteDto> registrarVoto(@PathVariable Long sessaoId,
                                                                @Valid @RequestBody DesembargadorPlenarioVotoRequest request,
                                                                Authentication authentication) {
        enforce(authentication, "desembargador_plenario_voto");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarVoto(sessaoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
