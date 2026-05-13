package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelBootstrapResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPainelWidgetsResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciaDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProximoEventoDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubDto;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.cidadao.CidadaoPainelService;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoPainelController {

    private final CidadaoPainelService service;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public CidadaoPainelController(CidadaoPainelService service, CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoPainelBootstrapResponse> painel(
            Authentication authentication,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel", ApiVersion.V1);
        CidadaoPainelBootstrapResponse resp = service.painelBootstrap();
        String etag = resp.etag();
        if (sameEtag(etag, ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(10)).cachePrivate())
                    .build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(10)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .eTag(etag)
                .body(resp);
    }

    @GetMapping("/painel/widgets")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoPainelWidgetsResponse> widgets(
            Authentication authentication,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel_widgets", ApiVersion.V1);
        CidadaoPainelWidgetsResponse resp = service.painelWidgets();
        String etag = resp.etag();
        if (sameEtag(etag, ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePrivate())
                    .build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .eTag(etag)
                .body(resp);
    }


    @GetMapping("/painel/processos-recentes")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<List<CidadaoProcessoCardDto>> processosRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel_processos_recentes", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(20)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(service.listarProcessosRecentes());
    }

    @GetMapping("/painel/pendencias")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<List<CidadaoPendenciaDto>> pendencias(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel_pendencias", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(15)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(service.listarPendencias());
    }

    @GetMapping("/painel/proximos-eventos")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<List<CidadaoProximoEventoDto>> proximosEventos(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel_proximos_eventos", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(15)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(service.listarProximosEventos());
    }

    @GetMapping("/painel/govhub")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoGovHubDto> govHub(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_painel_govhub", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(service.govHub());
    }



    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "citizen_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("CIDADAO"));
    }

    private static boolean sameEtag(String current, String candidate) {
        if (current == null || candidate == null) {
            return false;
        }
        return current.equals(candidate.trim());
    }
}
