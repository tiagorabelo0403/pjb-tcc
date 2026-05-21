package com.tcc.pjb.backend.controller.fazenda;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.fazenda.FazendaPublicaRemessaNecessariaRadarService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fazenda")
public class FazendaController {

    private final FazendaPublicaRemessaNecessariaRadarService remessaService;
    private final CapabilityRateLimiter rateLimiter;

    public FazendaController(FazendaPublicaRemessaNecessariaRadarService remessaService,
                             CapabilityRateLimiter rateLimiter) {
        this.remessaService = remessaService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/remessa-necessaria")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<FazendaPublicaRemessaNecessariaRadarService.RemessaResultado> avaliar(
            @Valid @RequestBody FazendaPublicaRemessaNecessariaRadarService.RemessaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "fazenda_remessa_necessaria", ApiVersion.V1);
        return ResponseEntity.ok(remessaService.avaliar(input));
    }

    @PostMapping("/remessa-necessaria/radar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<List<FazendaPublicaRemessaNecessariaRadarService.RemessaResultado>> radar(
            @Valid @RequestBody List<FazendaPublicaRemessaNecessariaRadarService.RemessaInput> processos,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "fazenda_remessa_necessaria_radar", ApiVersion.V1);
        return ResponseEntity.ok(remessaService.radar(processos));
    }
}
