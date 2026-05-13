package com.tcc.pjb.backend.controller.security;

import com.tcc.pjb.backend.configs.api.GovBrHttpResponses;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.model.dto.govbr.GovBrAssuranceLevelResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrStepUpStartResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.security.govbr.GovBrStepUpService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/auth/govbr")
public class GovBrStepUpController {

  private final CurrentUserService currentUser;
  private final ClientIpResolver ipResolver;
  private final GovBrStepUpService service;
  private final GovBrAssuranceExtractor govBrAssuranceExtractor;

  public GovBrStepUpController(CurrentUserService currentUser,
                               ClientIpResolver ipResolver,
                               GovBrStepUpService service,
                               GovBrAssuranceExtractor govBrAssuranceExtractor) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.ipResolver = Objects.requireNonNull(ipResolver);
    this.service = Objects.requireNonNull(service);
    this.govBrAssuranceExtractor = Objects.requireNonNull(govBrAssuranceExtractor);
  }

  @PostMapping(value = "/stepup/start", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public GovBrStepUpStartResponse start(@RequestHeader(name = "X-Device-ID", required = false) @Size(max = 64) String deviceIdHeader,
                                        HttpServletRequest request) {
    Usuario u = currentUser.getRequired();
    Long deviceId = parseLong(deviceIdHeader);
    if (deviceId == null) {
      Object attr = request.getAttribute("PJB_DEVICE_ID");
      if (attr instanceof Long l) {
        deviceId = l;
      } else if (attr instanceof String s) {
        deviceId = parseLong(s);
      }
    }

    String ip = ipResolver.resolve(request);
    var r = service.start(u, deviceId, ip);
    return new GovBrStepUpStartResponse(r.authorizeUrl(), r.expiresAt());
  }

  @GetMapping(value = "/assurance-level", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public GovBrAssuranceLevelResponse verificarNivel(Authentication auth) {
    String nivel = govBrAssuranceExtractor.extract(auth);
    return new GovBrAssuranceLevelResponse(
            nivel,
            "ouro".equals(nivel),
            "prata".equals(nivel) || "ouro".equals(nivel)
    );
  }

  @GetMapping("/stepup/callback")
  public ResponseEntity<Void> callback(@RequestParam(name = "code", required = false) @Size(max = 4096) String code,
                                       @RequestParam(name = "state", required = false) @Size(max = 128) String state) throws IOException, InterruptedException {
    String redirect = service.handleCallback(code, state);
    return GovBrHttpResponses.redirectOrNoContent(redirect);
  }

  private static Long parseLong(String v) {
    if (v == null) {
      return null;
    }
    String s = v.trim();
    if (s.isEmpty() || s.length() > 19 || !s.chars().allMatch(Character::isDigit)) {
      return null;
    }
    try {
      return Long.parseLong(s);
    } catch (Exception e) {
      return null;
    }
  }
}
