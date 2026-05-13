package com.tcc.pjb.backend.controller.magistratura;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActPreviewResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActWorkspaceResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialActWorkbenchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/magistratura")
public class MagistraturaJudicialActsController {

    private static final String MAGISTRATE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')";

    private final MagistraturaJudicialActWorkbenchService service;
    private final CapabilityRateLimiter rateLimiter;

    public MagistraturaJudicialActsController(MagistraturaJudicialActWorkbenchService service,
                                              CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/atos")
    @PreAuthorize(MAGISTRATE_ROLES)
    public ResponseEntity<MagistraturaJudicialActWorkspaceResponse> workspace(@RequestParam(value = "processoId", required = false) Long processoId,
                                                                              Authentication authentication) {
        enforce(authentication, "magistratura_atos_workspace");
        return ResponseEntity.ok(service.workspace(processoId));
    }

    @GetMapping("/processos/{processoId}/atos/preview")
    @PreAuthorize(MAGISTRATE_ROLES)
    public ResponseEntity<MagistraturaJudicialActPreviewResponse> preview(@PathVariable Long processoId,
                                                                          @RequestParam("action") String action,
                                                                          Authentication authentication) {
        enforce(authentication, "magistratura_atos_preview");
        return ResponseEntity.ok(service.preview(processoId, action));
    }

    @PostMapping("/processos/{processoId}/atos/automation-preview")
    @PreAuthorize(MAGISTRATE_ROLES)
    public ResponseEntity<MagistraturaJudicialActPreviewResponse> automationPreview(@PathVariable Long processoId,
                                                                                    @Valid @RequestBody MagistraturaJudicialActCommandRequest request,
                                                                                    Authentication authentication) {
        enforce(authentication, "magistratura_atos_automation_preview");
        return ResponseEntity.ok(service.preview(processoId, request));
    }

    @PostMapping("/processos/{processoId}/atos")
    @PreAuthorize(MAGISTRATE_ROLES)
    public ResponseEntity<MagistraturaJudicialActCommandResponse> execute(@PathVariable Long processoId,
                                                                          @Valid @RequestBody MagistraturaJudicialActCommandRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "magistratura_atos_execute");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.execute(processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
