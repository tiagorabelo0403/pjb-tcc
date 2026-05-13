package com.tcc.pjb.backend.modules.atendimento.controller;

import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoReportService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimento/reports")
public class AtendimentoReportController {

  private final AtendimentoReportService service;
  private final CapabilityRateLimiter rateLimiter;

  public AtendimentoReportController(AtendimentoReportService service, CapabilityRateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping(value = "/threads/{threadId}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO') or hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<byte[]> thread(Authentication authentication,
                                      @PathVariable("threadId") @Positive Long threadId,
                                      @RequestParam(value = "from", required = false) LocalDate from,
                                      @RequestParam(value = "to", required = false) LocalDate to) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_report_thread", ApiVersion.V1);
    byte[] pdf = service.threadPdf(threadId, from, to);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=thread_" + threadId + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  

  @GetMapping(value = "/threads/{threadId}/ata.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO') or hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<byte[]> threadAta(Authentication authentication,
                                         @PathVariable("threadId") @Positive Long threadId,
                                         @RequestParam(value = "from", required = false) LocalDate from,
                                         @RequestParam(value = "to", required = false) LocalDate to) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_ata_thread", ApiVersion.V1);
    byte[] pdf = service.threadAtaPdf(threadId, from, to);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ata_thread_" + threadId + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
@GetMapping(value = "/processos/{processoId}.zip", produces = "application/zip")
  @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO') or hasAnyAuthority('ROLE_ADMIN','ROLE_MAGISTRATURA','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM')")
  public ResponseEntity<byte[]> processo(Authentication authentication,
                                        @PathVariable("processoId") @Positive Long processoId,
                                        @RequestParam(value = "from", required = false) LocalDate from,
                                        @RequestParam(value = "to", required = false) LocalDate to) {
    rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_report_processo", ApiVersion.V1);
    byte[] zip = service.processoZip(processoId, from, to);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processo_" + processoId + "_atendimento.zip")
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(zip);
  }
}
