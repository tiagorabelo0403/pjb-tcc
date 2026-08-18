package com.tcc.pjb.backend.controller.recurso;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoDesertaoRadarService;
import com.tcc.pjb.backend.service.recurso.RecursoProcessualReadinessService;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recurso")
public class RecursoController {

    public record TempestividadeRequest(
            RecursoTempestividadeGuardService.TempestividadeInput tempestividade,
            CalendarioUteisService.CalendarioInput calendario
    ) {}

    private final RecursoAdmissibilidadeService admissibilidadeService;
    private final RecursoTempestividadeGuardService tempestividadeService;
    private final RecursoDesertaoRadarService desertaoService;
    private final RecursoProcessualReadinessService readinessService;
    private final CapabilityRateLimiter rateLimiter;

    public RecursoController(RecursoAdmissibilidadeService admissibilidadeService,
                             RecursoTempestividadeGuardService tempestividadeService,
                             RecursoDesertaoRadarService desertaoService,
                             RecursoProcessualReadinessService readinessService,
                             CapabilityRateLimiter rateLimiter) {
        this.admissibilidadeService = admissibilidadeService;
        this.tempestividadeService = tempestividadeService;
        this.desertaoService = desertaoService;
        this.readinessService = readinessService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/admissibilidade")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<RecursoAdmissibilidadeService.AdmissibilidadeResult> admissibilidade(
            @Valid @RequestBody RecursoAdmissibilidadeService.AdmissibilidadeInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "recurso_admissibilidade", ApiVersion.V1);
        return ResponseEntity.ok(admissibilidadeService.avaliar(input));
    }

    @PostMapping("/tempestividade")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<RecursoTempestividadeGuardService.TempestividadeResult> tempestividade(
            @Valid @RequestBody TempestividadeRequest request,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "recurso_tempestividade", ApiVersion.V1);
        return ResponseEntity.ok(tempestividadeService.avaliar(request.tempestividade(), request.calendario()));
    }

    @PostMapping("/desertao")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<RecursoDesertaoRadarService.DesertaoAnalise> desertao(
            @Valid @RequestBody RecursoDesertaoRadarService.DesertaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "recurso_desertao", ApiVersion.V1);
        return ResponseEntity.ok(desertaoService.analisar(input));
    }

    @PostMapping("/readiness")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<RecursoProcessualReadinessService.RecursoReadinessResult> readiness(
            @Valid @RequestBody RecursoProcessualReadinessService.RecursoReadinessInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "recurso_readiness", ApiVersion.V1);
        return ResponseEntity.ok(readinessService.avaliar(input));
    }
}
