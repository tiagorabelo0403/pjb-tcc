package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoJuntadaResumoDto;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.cidadao.CidadaoJuntadasService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cidadao/processos/{processoId}/juntadas")
public class CidadaoJuntadasController {

  private final CidadaoJuntadasService service;
  private final CapabilityRateLimiter rateLimiter;

  public CidadaoJuntadasController(CidadaoJuntadasService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping
  @PreAuthorize("hasRole('CIDADAO')")
  public ResponseEntity<List<CidadaoJuntadaResumoDto>> listar(
      Authentication authentication,
      @PathVariable("processoId") @Positive Long processoId,
      @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(200) int limit
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_juntadas", ApiVersion.V1);
    return ResponseEntity.ok(service.listar(processoId, limit));
  }
}
