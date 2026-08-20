package com.tcc.pjb.backend.controller.publico;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaProcessoViewResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.PublicPageResolveResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaSearchService;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/consultas-publicas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class ConsultasPublicasController {

    private final ConsultaPublicaSearchService consultaPublicaSearchService;
    private final ConsultaPublicaWorkspaceService consultaPublicaWorkspaceService;
    private final CapabilityRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/workspace")
    public ResponseEntity<ConsultaPublicaWorkspaceResponse> workspace(HttpServletRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_CONSULTA_WORKSPACE", ApiVersion.latest(), clientIpResolver.resolve(request));
        ConsultaPublicaWorkspaceResponse payload = consultaPublicaWorkspaceService.workspace();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(20)).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .eTag(payload.etag())
                .body(payload);
    }

    @GetMapping("/search")
    public ResponseEntity<ConsultaPublicaSearchResponse> search(
            @RequestParam("q") @NotBlank String q,
            @RequestParam(value = "tipoJustica", required = false) String tipoJustica,
            @RequestParam(value = "ramoDireito", required = false) String ramoDireito,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_CONSULTA_SEARCH", ApiVersion.latest(), clientIpResolver.resolve(request));
        ConsultaPublicaSearchResponse payload = consultaPublicaSearchService.searchPublic(q, tipoJustica, ramoDireito, page, size);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(15)).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(payload);
    }

    @GetMapping("/processos/{numero}")
    public ResponseEntity<ConsultaPublicaProcessoViewResponse> detail(
            @PathVariable @NotBlank String numero,
            HttpServletRequest request
    ) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_CONSULTA_PROCESSO_DETAIL", ApiVersion.latest(), clientIpResolver.resolve(request));
        ConsultaPublicaProcessoViewResponse payload = consultaPublicaWorkspaceService.detail(numero);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(25)).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .eTag(payload.etag())
                .body(payload);
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<PublicPageResolveResponse> resolvePage(
            @PathVariable @NotBlank String pageId,
            HttpServletRequest request
    ) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, SecurityContextHolder.getContext().getAuthentication(), "PUBLIC_CONSULTA_PAGE_RESOLVE", ApiVersion.latest(), clientIpResolver.resolve(request));
        PublicPageResolveResponse payload = consultaPublicaSearchService.resolvePublicPage(pageId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(payload);
    }
}
