package com.tcc.pjb.backend.controller.processual.document.template;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;

@RestController
@RequestMapping("/api/v1/processual/documentos-oficiais")
public class OfficialDocumentTemplateController {

    private final OfficialDocumentTemplateService service;

    public OfficialDocumentTemplateController(OfficialDocumentTemplateService service) {
        this.service = service;
    }

    @PostMapping("/renderizar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OfficialDocumentTemplateRenderResponse> renderizar(@Valid @RequestBody OfficialDocumentTemplateRenderRequest request) {
        return ResponseEntity.ok(service.renderizar(request));
    }
}
