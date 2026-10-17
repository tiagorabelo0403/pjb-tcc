package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPastaProcessosResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.cidadao.CidadaoPastaProcessosService;

@RestController
@RequestMapping("/api/v1/cidadao/pasta")
@Validated
public class CidadaoPastaProcessosController {

  private final CidadaoPastaProcessosService service;
  private final CapabilityRateLimiter rateLimiter;

  public CidadaoPastaProcessosController(CidadaoPastaProcessosService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/processos")
  @PreAuthorize("hasRole('CIDADAO')")
  public ResponseEntity<CidadaoPastaProcessosResponse> listar(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(value = "numero", required = false) String numero,
      @RequestParam(value = "uf", required = false) String uf,
      @RequestParam(value = "status", required = false) String status
  ) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "cidadao_meus_processos", ApiVersion.V1);
    int safeSize = Math.max(1, Math.min(size, 100));
    Pageable pageable = PageRequest.of(
        Math.max(page, 0),
        safeSize,
        Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao").and(Sort.by(Sort.Direction.DESC, "id"))
    );
    return ResponseEntity.ok(service.listar(pageable, numero, uf, status));
  }
}
