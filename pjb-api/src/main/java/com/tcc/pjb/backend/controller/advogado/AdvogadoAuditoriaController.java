package com.tcc.pjb.backend.controller.advogado;

import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoAuditDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.advogado.AdvogadoAuditoriaLedgerService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/advogado/auditoria")
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class AdvogadoAuditoriaController {

    private final AdvogadoAuditoriaLedgerService ledgerService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public AdvogadoAuditoriaController(AdvogadoAuditoriaLedgerService ledgerService,
                                       CurrentUserService currentUserService,
                                       CapabilityRateLimiter rateLimiter) {
        this.ledgerService = Objects.requireNonNull(ledgerService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/ledger")
    public ResponseEntity<Page<AdvogadoAuditDto.LedgerEventResponse>> ledger(
            Authentication authentication,
            @RequestParam(defaultValue = "ADV_") String actionPrefix,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            Pageable pageable
    ) {
        enforce(authentication, "advogado_audit_ledger");

        Usuario solicitante = currentUserService.getRequired();

        return ResponseEntity.ok(ledgerService.doAdvogado(
                solicitante.getId(), actionPrefix, resourceType, resourceId, pageable));
    }

    private void enforce(Authentication authentication, String key) {
        if (rateLimiter != null) {
            rateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, authentication, key, ApiVersion.V1);
        }
    }
}
