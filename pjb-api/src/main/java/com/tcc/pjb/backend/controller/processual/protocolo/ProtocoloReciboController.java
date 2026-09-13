package com.tcc.pjb.backend.controller.processual.protocolo;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboConsultaService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/ajuizamento/protocolos")
@PreAuthorize("isAuthenticated()")
public class ProtocoloReciboController {

    private final ProtocoloReciboConsultaService protocoloReciboConsultaService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public ProtocoloReciboController(ProtocoloReciboConsultaService protocoloReciboConsultaService,
                                     CurrentUserService currentUserService,
                                     CapabilityRateLimiter rateLimiter) {
        this.protocoloReciboConsultaService = Objects.requireNonNull(protocoloReciboConsultaService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping("/{processoId}/recibo")
    public ResponseEntity<ProtocoloReciboResponse> recibo(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "PROTOCOLO_RECIBO", ApiVersion.latest());
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(protocoloReciboConsultaService.reciboDoProcesso(processoId, usuario));
    }
}
