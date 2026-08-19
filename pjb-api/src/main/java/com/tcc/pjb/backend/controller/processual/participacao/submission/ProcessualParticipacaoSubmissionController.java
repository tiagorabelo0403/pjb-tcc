package com.tcc.pjb.backend.controller.processual.participacao.submission;

import com.tcc.pjb.backend.controller.processual.participacao.support.ProcessualParticipacaoControllerRateLimitSupport;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.processual.participacao.ProcessualParticipacaoAtivaFacadeService;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionResponse;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.PROCESSUAL_PARTICIPACAO_ATIVA_BASE)
@PreAuthorize("isAuthenticated()")
public class ProcessualParticipacaoSubmissionController {

    private final ProcessualParticipacaoAtivaFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final CapabilityRateLimitDomainResolver domainResolver;

    public ProcessualParticipacaoSubmissionController(ProcessualParticipacaoAtivaFacadeService facadeService,
                                                      CapabilityRateLimiter rateLimiter,
                                                      CapabilityRateLimitDomainResolver domainResolver) {
        this.facadeService = Objects.requireNonNull(facadeService, "facadeService");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.domainResolver = Objects.requireNonNull(domainResolver, "domainResolver");
    }

    @PostMapping(OperationalApiRoutes.PATH_PROCESSUAL_PARTICIPACAO_PROTOCOLAR)
    public ResponseEntity<SubmissionResponse> protocolar(Authentication authentication,
                                                         @PathVariable("processoId") @Positive Long processoId,
                                                         @Valid @RequestBody SubmissionRequest request) {
        ProcessualParticipacaoControllerRateLimitSupport.enforce(
                rateLimiter,
                domainResolver,
                authentication,
                "processual_participacao_protocolar");
        return ResponseEntity.ok(facadeService.protocolar(processoId, request));
    }

    @GetMapping(OperationalApiRoutes.PATH_PROCESSUAL_PARTICIPACAO_SUBMISSOES)
    public ResponseEntity<List<SubmissionView>> listarMinhasSubmissoes(Authentication authentication,
                                                                       @PathVariable("processoId") @Positive Long processoId,
                                                                       @RequestParam(value = "limit", defaultValue = "12")
                                                                       @Min(1) @Max(50) int limit) {
        ProcessualParticipacaoControllerRateLimitSupport.enforce(
                rateLimiter,
                domainResolver,
                authentication,
                "processual_participacao_listar_submissoes");
        return ResponseEntity.ok(facadeService.listarMinhasSubmissoes(processoId, limit));
    }
}
