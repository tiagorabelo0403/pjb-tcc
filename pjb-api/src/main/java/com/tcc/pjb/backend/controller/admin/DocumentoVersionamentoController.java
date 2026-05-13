package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.DocumentoVersionamentoRequest;
import com.tcc.pjb.backend.model.dto.governance.DocumentoVersionamentoResponse;
import com.tcc.pjb.backend.service.governance.DocumentoVersionamentoService;

@RestController
@RequestMapping("/api/v1/admin/governance/document-versioning")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public class DocumentoVersionamentoController {

    private final DocumentoVersionamentoService service;

    public DocumentoVersionamentoController(DocumentoVersionamentoService service) {
        this.service = service;
    }

    @PostMapping("/history")
    public ResponseEntity<DocumentoVersionamentoResponse> history(@Valid @RequestBody DocumentoVersionamentoRequest request) {
        return ResponseEntity.ok(service.historico(
                request.processoId(),
                request.tituloBase(),
                request.retificacao(),
                request.bloqueadoPorAssinatura()
        ));
    }
}
