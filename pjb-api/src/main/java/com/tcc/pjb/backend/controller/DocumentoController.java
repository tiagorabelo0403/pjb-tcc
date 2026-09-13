package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.service.document.DocumentoPdfDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentoController {

    private final DocumentoPdfDownloadService pdfDownloadService;

    @GetMapping(value = "/{documentoId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPdf(@PathVariable UUID documentoId, HttpServletRequest request) {
        var autorizado = pdfDownloadService.resolver(documentoId, processoId -> {
            if (request != null) {
                request.setAttribute("PJB_PROCESSO_ID", processoId);
                request.setAttribute("PJB_DOCUMENTO_ID", documentoId.toString());
            }
        });

        var resolved = autorizado.conteudo();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento_" + documentoId + ".pdf\"");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Robots-Tag", "noindex, nofollow, noarchive");
        // Sem efeito hoje: o ResourceHttpMessageConverter sobrescreve com "bytes". Mantido porque a
        // decisao entre tornar efetivo e assumir a faixa esta registrada em
        // D-accept-ranges-declarado-e-sobrescrito-no-download-de-pdf, e nao cabe nesta fatia.
        headers.set("Accept-Ranges", "none");

        if (resolved.contentLength() > 0 && resolved.contentLength() < Integer.MAX_VALUE) {
            headers.setContentLength(resolved.contentLength());
        }

        headers.setCacheControl("no-store, no-cache, max-age=0, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok().headers(headers).body(resolved.resource());
    }
}
