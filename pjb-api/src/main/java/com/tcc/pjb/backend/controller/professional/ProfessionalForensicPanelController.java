package com.tcc.pjb.backend.controller.professional;

import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicAccessMatrixResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicClient360Response;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicInstitutionalOverviewResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicPanelWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicProcessDetailResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicSearchResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.professional.ProfessionalForensicPanelService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/professional/forensic-panel")
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public class ProfessionalForensicPanelController {

    private final ProfessionalForensicPanelService service;
    private final CapabilityRateLimiter rateLimiter;

    public ProfessionalForensicPanelController(ProfessionalForensicPanelService service,
                                               CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/workspace")
    public ResponseEntity<ProfessionalForensicPanelWorkspaceResponse> workspace() {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_WORKSPACE", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(10)).cachePrivate().mustRevalidate())
                .body(service.workspace());
    }


    @GetMapping("/institutional-overview")
    public ResponseEntity<ProfessionalForensicInstitutionalOverviewResponse> institutionalOverview(@RequestParam(value = "uf", required = false) String uf,
                                                                                                    @RequestParam(value = "comarca", required = false) String comarca,
                                                                                                    @RequestParam(value = "limit", defaultValue = "12") int limit) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_OVERVIEW", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(8)).cachePrivate().mustRevalidate())
                .body(service.institutionalOverview(uf, comarca, limit));
    }

    @GetMapping("/client-360")
    public ResponseEntity<ProfessionalForensicClient360Response> client360(@RequestParam(value = "nome", required = false) String nome,
                                                                           @RequestParam(value = "cpf", required = false) String cpf,
                                                                           @RequestParam(value = "numero", required = false) String numero,
                                                                           @RequestParam(value = "uf", required = false) String uf,
                                                                           @RequestParam(value = "comarca", required = false) String comarca,
                                                                           @RequestParam(value = "size", defaultValue = "24") int size,
                                                                           HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_CLIENT_360", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.client360(nome, cpf, numero, uf, comarca, size, fingerprint(request)));
    }

    @GetMapping("/process-search")
    public ResponseEntity<ProfessionalForensicSearchResponse> search(@RequestParam(value = "nome", required = false) String nome,
                                                                     @RequestParam(value = "cpf", required = false) String cpf,
                                                                     @RequestParam(value = "numero", required = false) String numero,
                                                                     @RequestParam(value = "uf", required = false) String uf,
                                                                     @RequestParam(value = "comarca", required = false) String comarca,
                                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                                     @RequestParam(value = "size", defaultValue = "20") int size,
                                                                     HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_SEARCH", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.search(nome, cpf, numero, uf, comarca, page, size, fingerprint(request)));
    }

    @GetMapping("/processos/{numero}/access-matrix")
    public ResponseEntity<ProfessionalForensicAccessMatrixResponse> accessMatrix(@PathVariable String numero) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_ACCESS_MATRIX", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.accessMatrix(numero));
    }

    @GetMapping("/processos/{numero}")
    public ResponseEntity<ProfessionalForensicProcessDetailResponse> detail(@PathVariable String numero,
                                                                            HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_FORENSIC_DETAIL", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.detail(numero, fingerprint(request)));
    }

    private String fingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remote = request.getRemoteAddr();
        String agent = request.getHeader("User-Agent");
        String session = request.getSession(false) == null ? null : request.getSession(false).getId();
        return String.join("|",
                remote == null ? "" : remote,
                agent == null ? "" : agent,
                session == null ? "" : session);
    }
}
