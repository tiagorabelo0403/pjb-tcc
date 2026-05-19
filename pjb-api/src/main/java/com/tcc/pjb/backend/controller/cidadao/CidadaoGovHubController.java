package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGovHubDto;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.cidadao.CidadaoGovHubService;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/cidadao")
public class CidadaoGovHubController {

    private final CidadaoGovHubService service;
    private final CapabilityRateLimiter rateLimiter;

    public CidadaoGovHubController(CidadaoGovHubService service, CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/gov-hub", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoGovHubDto> hub(
            Authentication authentication,
            @RequestParam(name = "uf", required = false) @Size(max = 8) String uf,
            @RequestHeader(name = "If-None-Match", required = false) @Size(max = 256) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_govhub", ApiVersion.V1);

        CidadaoGovHubService.CidadaoGovHubCacheToken cacheToken = service.cacheTokenForUf(uf);
        if (cacheToken.matches(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate())
                    .eTag(cacheToken.etag())
                    .build();
        }

        CidadaoGovHubDto dto = (uf != null && !uf.isBlank()) ? service.hubForUf(uf.trim()) : service.hubForCurrentUser();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate())
                .eTag(cacheToken.etag())
                .body(dto);
    }
}
