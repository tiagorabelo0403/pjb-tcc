package com.tcc.pjb.backend.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.pastadigital.*;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.pastadigital.PastaDigitalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PastaDigitalController {

    private final PastaDigitalService pastaDigitalService;
    private final CurrentUserService currentUserService;

    
    @PostMapping(value = "/processos/{processoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoIndexadoResponse> anexarDocumento(
            @PathVariable Long processoId,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestPart(value = "titulo", required = false) String titulo,
            @RequestParam(value = "origem", required = false) String origem,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "nivelSigiloDoc", required = false) String nivelSigiloDoc
    ) {
        Usuario u = currentUserService.getRequired();
        DocumentoIndexadoResponse resp = pastaDigitalService.anexarDocumentoPdf(
                processoId,
                arquivo,
                titulo,
                u != null ? u.getId() : null,
                origem,
                categoria,
                nivelSigiloDoc
        );
        return ResponseEntity.ok(resp);
    }

    
    @GetMapping("/processos/{processoId}/pasta-digital")
    public ResponseEntity<PastaDigitalResponse> pastaDigital(@PathVariable Long processoId) {
        return ResponseEntity.ok(pastaDigitalService.pastaDigital(processoId));
    }

    
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<PageResolveResponse> resolverPageId(@PathVariable @NotBlank String pageId) {
        return ResponseEntity.ok(pastaDigitalService.resolverPageId(pageId));
    }

    
    @GetMapping("/processos/{processoId}/pasta-digital/busca")
    public ResponseEntity<PageSearchResponse> buscar(
            @PathVariable Long processoId,
            @RequestParam("q") String q,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(pastaDigitalService.buscarNoProcesso(processoId, q, limit));
    }
}
