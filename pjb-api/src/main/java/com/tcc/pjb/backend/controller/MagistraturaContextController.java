package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaContextResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaOperationalContextResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.magistratura.surface.MagistraturaContextSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magistratura")
public class MagistraturaContextController {

    private final MagistraturaContextSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MagistraturaContextController(MagistraturaContextSurfaceFacadeService facadeService,
                                         CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/context")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<MagistraturaContextResponse> context(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "magistratura_context", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.context());
    }

    @GetMapping("/operacional")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<MagistraturaOperationalContextResponse> operacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "magistratura_operacional", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.operacional());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/recentes")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> consultasRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "magistratura_localizador_consultas_recentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.consultasRecentes());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/metricas")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> metricasLocalizador(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "magistratura_localizador_metricas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.metricas());
    }

    @PostMapping("/inteligencia/pessoas/localizacao")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<PessoaLocalizacaoResponse> localizarPessoa(@Valid @RequestBody PessoaLocalizacaoRequest request,
                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "magistratura_localizador_pessoas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.localizarPessoa(request));
    }
}
