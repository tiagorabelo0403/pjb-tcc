package com.tcc.pjb.backend.controller.auth;

import com.tcc.pjb.backend.configs.api.GovBrHttpResponses;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginSessionResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginStartResponse;
import com.tcc.pjb.backend.service.security.govbr.GovBrLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/auth/govbr/login")
@PreAuthorize("permitAll()")
public class GovBrLoginController {

  private final GovBrLoginService service;

  public GovBrLoginController(GovBrLoginService service) {
    this.service = Objects.requireNonNull(service);
  }

  @PostMapping(value = "/iniciar", produces = MediaType.APPLICATION_JSON_VALUE)
  public GovBrLoginStartResponse iniciar() {
    return service.start();
  }

  @GetMapping("/callback")
  public ResponseEntity<Void> callback(
      @RequestParam(name = "code", required = false) @Size(max = 4096) String code,
      @RequestParam(name = "state", required = false) @Size(max = 128) String state) throws IOException, InterruptedException {
    String redirect = service.handleCallback(code, state);
    return GovBrHttpResponses.redirectOrNoContent(redirect);
  }

  @GetMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
  public GovBrLoginSessionResponse session(
      @RequestParam(name = "state") @Size(max = 128) String state,
      HttpServletRequest servletRequest) {
    return service.retrieveSession(state, servletRequest.getRemoteAddr());
  }
}
