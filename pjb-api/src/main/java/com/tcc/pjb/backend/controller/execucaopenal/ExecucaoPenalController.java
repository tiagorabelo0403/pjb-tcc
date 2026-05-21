package com.tcc.pjb.backend.controller.execucaopenal;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService;
import com.tcc.pjb.backend.service.execucaopenal.PenaPrivativaCalculatorService;
import com.tcc.pjb.backend.service.processual.acceleration.penal.PenalAudienciaInstrucaoReadinessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/execucaopenal")
public class ExecucaoPenalController {

    private final ExecucaoPenalChecklistService checklistService;
    private final PenaPrivativaCalculatorService calculatorService;
    private final PenalAudienciaInstrucaoReadinessService audienciaService;
    private final CapabilityRateLimiter rateLimiter;

    public ExecucaoPenalController(ExecucaoPenalChecklistService checklistService,
                                   PenaPrivativaCalculatorService calculatorService,
                                   PenalAudienciaInstrucaoReadinessService audienciaService,
                                   CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.calculatorService = calculatorService;
        this.audienciaService = audienciaService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<ExecucaoPenalChecklistService.ExecucaoPenalResult> checklist(
            @Valid @RequestBody ExecucaoPenalChecklistService.ExecucaoPenalInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "execucaopenal_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }

    @PostMapping("/pena-calculadora")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<PenaPrivativaCalculatorService.PenaUnificada> penaCalculadora(
            @Valid @RequestBody PenaPrivativaCalculatorService.PenaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "execucaopenal_pena_calculadora", ApiVersion.V1);
        return ResponseEntity.ok(calculatorService.unificar(input));
    }

    @PostMapping("/audiencia-readiness")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<PenalAudienciaInstrucaoReadinessService.ReadinessSnapshot> audienciaReadiness(
            @Valid @RequestBody PenalAudienciaInstrucaoReadinessService.ReadinessInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "execucaopenal_audiencia_readiness", ApiVersion.V1);
        return ResponseEntity.ok(audienciaService.avaliar(input));
    }
}
