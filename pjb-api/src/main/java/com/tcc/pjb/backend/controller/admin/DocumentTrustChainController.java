package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.DocumentTrustChainReportResponse;
import com.tcc.pjb.backend.model.dto.governance.DocumentTrustChainSealRequest;
import com.tcc.pjb.backend.model.dto.governance.DocumentTrustChainSealResponse;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;

@RestController
@RequestMapping("/api/v1/admin/governance/document-trust-chain")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
public class DocumentTrustChainController {

    private final DocumentTrustChainService service;

    public DocumentTrustChainController(DocumentTrustChainService service) {
        this.service = service;
    }

    @PostMapping("/seal")
    public ResponseEntity<DocumentTrustChainSealResponse> seal(@Valid @RequestBody DocumentTrustChainSealRequest request) {
        return ResponseEntity.ok(service.selar(
                request.processoId(),
                request.documentoId(),
                request.loteReferencia(),
                request.motivo(),
                request.contrassinado(),
                request.preservaSigilo(),
                request.nivelAssinatura()
        ));
    }

    @GetMapping("/{processoId}/report")
    public ResponseEntity<DocumentTrustChainReportResponse> report(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.analisarProcesso(processoId));
    }
}
