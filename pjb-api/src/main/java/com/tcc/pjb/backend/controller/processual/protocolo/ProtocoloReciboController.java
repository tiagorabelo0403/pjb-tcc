package com.tcc.pjb.backend.controller.processual.protocolo;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboService;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/processual/ajuizamento/protocolos")
@PreAuthorize("isAuthenticated()")
public class ProtocoloReciboController {

    private final ProcessoRepository processoRepository;
    private final ProtocoloReciboService protocoloReciboService;
    private final PjbAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public ProtocoloReciboController(ProcessoRepository processoRepository,
                                     ProtocoloReciboService protocoloReciboService,
                                     PjbAuthorizationService authorizationService,
                                     CurrentUserService currentUserService,
                                     CapabilityRateLimiter rateLimiter) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.protocoloReciboService = Objects.requireNonNull(protocoloReciboService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping("/{processoId}/recibo")
    public ResponseEntity<ProtocoloReciboResponse> recibo(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "PROTOCOLO_RECIBO", ApiVersion.latest());
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        AuthzDecision decisao = authorizationService.canReadProcesso(processo);
        if (!decisao.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, decisao.reason());
        }
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(protocoloReciboService.obterOuEmitirRecibo(processo, usuario));
    }
}
