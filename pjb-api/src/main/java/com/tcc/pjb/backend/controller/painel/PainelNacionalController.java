package com.tcc.pjb.backend.controller.painel;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.AlertaPrazo;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.EventoPainelCommand;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.MetricaTribunal;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.NivelAlerta;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.PontoSerieTemporalDiaria;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService.SnapshotNacional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/painel/nacional")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR','ROLE_MAGISTRADO','ROLE_SERVIDOR_JUDICIARIO')")
public class PainelNacionalController {

    private final PainelNacionalJusticaService painelService;
    private final CapabilityRateLimiter rateLimiter;

    public PainelNacionalController(PainelNacionalJusticaService painelService,
                                    CapabilityRateLimiter rateLimiter) {
        this.painelService = Objects.requireNonNull(painelService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping("/snapshot")
    public ResponseEntity<SnapshotNacional> snapshot(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "painel_nacional_snapshot", ApiVersion.V1);
        return ResponseEntity.ok(painelService.gerarSnapshot());
    }

    @GetMapping("/tribunal")
    public ResponseEntity<MetricaTribunal> tribunal(@RequestParam("codigo") String codigo,
                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "painel_nacional_tribunal", ApiVersion.V1);
        return painelService.metricasTribunal(codigo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/serie-temporal")
    public ResponseEntity<List<PontoSerieTemporalDiaria>> serieTemporal(
            @RequestParam(value = "dias", defaultValue = "30") int dias,
            @RequestParam(value = "tribunal", required = false) String tribunalCodigo,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "painel_nacional_serie", ApiVersion.V1);
        return ResponseEntity.ok(painelService.serieTemporal(dias, tribunalCodigo));
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaPrazo>> alertas(
            @RequestParam(value = "nivel", defaultValue = "BAIXO") String nivel,
            @RequestParam(value = "tribunal", required = false) String tribunalCodigo,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "painel_nacional_alertas", ApiVersion.V1);
        NivelAlerta nivelAlerta = parseNivel(nivel);
        return ResponseEntity.ok(painelService.alertasPrazo(nivelAlerta, tribunalCodigo));
    }

    @PostMapping("/evento")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> registrarEvento(@Valid @RequestBody EventoPainelCommand command,
                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "painel_nacional_evento", ApiVersion.V1);
        painelService.registrarEvento(command);
        return ResponseEntity.accepted().build();
    }

    private NivelAlerta parseNivel(String nivel) {
        try {
            return NivelAlerta.valueOf(nivel.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NivelAlerta.BAIXO;
        }
    }
}
