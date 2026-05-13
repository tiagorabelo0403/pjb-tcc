package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.configs.api.GovBrHttpResponses;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLinkStartResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.cidadao.govbr.CidadaoGovBrLinkService;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/cidadao/govbr")
public class CidadaoGovBrLinkController {

  private final CurrentUserService currentUser;
  private final CidadaoGovBrLinkService service;
  private final CapabilityRateLimiter rateLimiter;

  public CidadaoGovBrLinkController(CurrentUserService currentUser,
                                    CidadaoGovBrLinkService service,
                                    CapabilityRateLimiter rateLimiter) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.service = Objects.requireNonNull(service);
    this.rateLimiter = Objects.requireNonNull(rateLimiter);
  }

  @PostMapping(value = "/link/start", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('CIDADAO')")
  public GovBrLinkStartResponse start(Authentication authentication) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "govbr_link_start", ApiVersion.V1);
    Usuario u = currentUser.getRequired();
    if (u.getTipoUsuario() != TipoUsuario.CIDADAO) {
      throw new IllegalStateException("role");
    }
    String url = service.startLink(u);
    return new GovBrLinkStartResponse(url);
  }

  @GetMapping("/link/callback")
  public ResponseEntity<Void> callback(@RequestParam(name = "code", required = false) @Size(max = 4096) String code,
      @RequestParam(name = "state", required = false) @Size(max = 128) String state) throws IOException, InterruptedException {
    String redirect = service.handleCallback(code, state);
    return GovBrHttpResponses.redirectOrNoContent(redirect);
  }
}
