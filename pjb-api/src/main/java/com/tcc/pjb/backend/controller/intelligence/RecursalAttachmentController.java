package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.model.dto.intelligence.RecursalAttachmentUploadResponse;
import com.tcc.pjb.backend.service.intelligence.surface.RecursalAttachmentSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/intelligence/recursal")
@ConditionalOnProperty(prefix = "pjb.recursal.attachments", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("isAuthenticated()")
public class RecursalAttachmentController {

    private final RecursalAttachmentSurfaceFacadeService facadeService;

    public RecursalAttachmentController(RecursalAttachmentSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping(path = "/processo/{processoId}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecursalAttachmentUploadResponse> upload(Authentication authentication,
                                                                   @PathVariable Long processoId,
                                                                   @RequestParam("file") MultipartFile file) {
        return facadeService.upload(authentication, processoId, file);
    }

    @GetMapping("/processo/{processoId}/attachments/{filename}")
    public ResponseEntity<InputStreamResource> download(Authentication authentication,
                                                        @PathVariable Long processoId,
                                                        @PathVariable String filename,
                                                        HttpServletRequest request) {
        if (request != null) {
            request.setAttribute("PJB_PROCESSO_ID", processoId);
        }
        return facadeService.download(authentication, processoId, filename);
    }
}
