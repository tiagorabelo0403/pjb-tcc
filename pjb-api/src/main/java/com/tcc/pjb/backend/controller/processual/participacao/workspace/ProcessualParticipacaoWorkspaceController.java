package com.tcc.pjb.backend.controller.processual.participacao.workspace;

import com.tcc.pjb.backend.controller.processual.participacao.support.ProcessualParticipacaoControllerRateLimitSupport;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.processual.participacao.ProcessualParticipacaoAtivaFacadeService;
import com.tcc.pjb.backend.service.processual.participacao.workspace.WorkspaceView;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.PROCESSUAL_PARTICIPACAO_ATIVA_BASE)
@PreAuthorize("isAuthenticated()")
public class ProcessualParticipacaoWorkspaceController {

    private final ProcessualParticipacaoAtivaFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final CapabilityRateLimitDomainResolver domainResolver;

    public ProcessualParticipacaoWorkspaceController(ProcessualParticipacaoAtivaFacadeService facadeService,
                                                     CapabilityRateLimiter rateLimiter,
                                                     CapabilityRateLimitDomainResolver domainResolver) {
        this.facadeService = Objects.requireNonNull(facadeService, "facadeService");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.domainResolver = Objects.requireNonNull(domainResolver, "domainResolver");
    }

    @GetMapping(OperationalApiRoutes.PATH_PROCESSUAL_PARTICIPACAO_WORKSPACE)
    public ResponseEntity<WorkspaceView> workspace(Authentication authentication,
                                                   @PathVariable("processoId") @Positive Long processoId) {
        ProcessualParticipacaoControllerRateLimitSupport.enforce(
                rateLimiter,
                domainResolver,
                authentication,
                "processual_participacao_workspace");
        return ResponseEntity.ok(facadeService.workspace(processoId));
    }
}
