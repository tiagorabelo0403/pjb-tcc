package com.tcc.pjb.backend.modules.atendimento.controller;

import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDownloadDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDto;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoAttachmentService;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/atendimento")
public class AtendimentoAttachmentController {

    private final AtendimentoAttachmentService service;
    private final CapabilityRateLimiter rateLimiter;

    public AtendimentoAttachmentController(AtendimentoAttachmentService service, CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping(value = "/threads/{threadId}/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<AtendimentoAttachmentDto> upload(Authentication authentication,
                                                           @PathVariable("threadId") @Positive Long threadId,
                                                           @RequestParam("file") MultipartFile file) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_attachment_upload", ApiVersion.V1);
        return ResponseEntity.ok(service.upload(threadId, file));
    }

    @GetMapping("/threads/{threadId}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<AtendimentoAttachmentDto> meta(Authentication authentication,
                                                         @PathVariable("threadId") @Positive Long threadId,
                                                         @PathVariable("attachmentId") @Positive Long attachmentId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_attachment_meta", ApiVersion.V1);
        return ResponseEntity.ok(service.meta(threadId, attachmentId));
    }

    @GetMapping("/threads/{threadId}/attachments/{attachmentId}/download-url")
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<AtendimentoAttachmentDownloadDto> downloadUrl(Authentication authentication,
                                                                        @PathVariable("threadId") @Positive Long threadId,
                                                                        @PathVariable("attachmentId") @Positive Long attachmentId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_attachment_download", ApiVersion.V1);
        return ResponseEntity.ok(service.downloadUrl(threadId, attachmentId));
    }

    @GetMapping("/threads/{threadId}/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<Void> download(Authentication authentication,
                                         @PathVariable("threadId") @Positive Long threadId,
                                         @PathVariable("attachmentId") @Positive Long attachmentId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_attachment_download", ApiVersion.V1);
        AtendimentoAttachmentDownloadDto dto = service.downloadUrl(threadId, attachmentId);
        URI uri = URI.create(dto.url());
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, uri.toString())
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @GetMapping(value = "/threads/{threadId}/attachments/{attachmentId}/download-secure", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('CIDADAO','ADVOGADO')")
    public ResponseEntity<Resource> downloadSecure(Authentication authentication,
                                                   jakarta.servlet.http.HttpServletRequest request,
                                                   @PathVariable("threadId") @Positive Long threadId,
                                                   @PathVariable("attachmentId") @Positive Long attachmentId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, authentication, "atendimento_attachment_download_secure", ApiVersion.V1);
        request.setAttribute("PJB_DOCUMENTO_ID", "att-" + attachmentId);
        var r = service.downloadSecure(threadId, attachmentId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"anexo.pdf\"")
            .cacheControl(CacheControl.noStore())
            .contentLength(r.contentLength())
            .body(r.resource());
    }
}
