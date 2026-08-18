package com.tcc.pjb.backend.controller.sentenca;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.civil.CivilMinutaSentencaPreparationService;
import com.tcc.pjb.backend.service.processual.execucao.CumprimentoSentencaReadinessService;
import com.tcc.pjb.backend.service.sentenca.ExequibilidadeDecisaoService;
import com.tcc.pjb.backend.service.sentenca.SentencaReadinessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sentenca")
public class SentencaController {

    private final SentencaReadinessService readinessService;
    private final ExequibilidadeDecisaoService exequibilidadeService;
    private final CumprimentoSentencaReadinessService cumprimentoService;
    private final CivilMinutaSentencaPreparationService minutaService;
    private final CapabilityRateLimiter rateLimiter;

    public SentencaController(SentencaReadinessService readinessService,
                              ExequibilidadeDecisaoService exequibilidadeService,
                              CumprimentoSentencaReadinessService cumprimentoService,
                              CivilMinutaSentencaPreparationService minutaService,
                              CapabilityRateLimiter rateLimiter) {
        this.readinessService = readinessService;
        this.exequibilidadeService = exequibilidadeService;
        this.cumprimentoService = cumprimentoService;
        this.minutaService = minutaService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/readiness")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ASSESSOR_JUDICIAL')")
    public ResponseEntity<SentencaReadinessService.SentencaReadinessResult> readiness(
            @Valid @RequestBody SentencaReadinessService.SentencaReadinessInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "sentenca_readiness", ApiVersion.V1);
        return ResponseEntity.ok(readinessService.avaliar(input));
    }

    @PostMapping("/exequibilidade")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<ExequibilidadeDecisaoService.ExequibilidadeResult> exequibilidade(
            @Valid @RequestBody ExequibilidadeDecisaoService.ExequibilidadeInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "sentenca_exequibilidade", ApiVersion.V1);
        return ResponseEntity.ok(exequibilidadeService.avaliar(input));
    }

    @PostMapping("/cumprimento")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<CumprimentoSentencaReadinessService.CumprimentoSnapshot> cumprimento(
            @Valid @RequestBody CumprimentoSentencaReadinessService.CumprimentoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "sentenca_cumprimento", ApiVersion.V1);
        return ResponseEntity.ok(cumprimentoService.avaliar(input));
    }

    @PostMapping("/minuta")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','ASSESSOR_JUDICIAL','SERVIDOR_FORUM')")
    public ResponseEntity<CivilMinutaSentencaPreparationService.MinutaPreparada> minuta(
            @Valid @RequestBody CivilMinutaSentencaPreparationService.MinutaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "sentenca_minuta", ApiVersion.V1);
        return ResponseEntity.ok(minutaService.preparar(input));
    }
}
