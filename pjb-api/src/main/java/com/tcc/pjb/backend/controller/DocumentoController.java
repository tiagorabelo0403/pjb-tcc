package com.tcc.pjb.backend.controller;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentoController {

    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final PjbAuthorizationService authorizationService;
    private final DocumentContentService contentService;
    private final RecursalEffectiveSecrecyService secrecyService;

    @GetMapping(value = "/{documentoId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPdf(@PathVariable UUID documentoId, HttpServletRequest request) {
        DocumentoProcessual d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));

        Long processoId = (d.getProcesso() != null) ? d.getProcesso().getId() : null;
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", "(n/a)");
        }

        if (request != null) {
            request.setAttribute("PJB_PROCESSO_ID", processoId);
            request.setAttribute("PJB_DOCUMENTO_ID", documentoId.toString());
        }

        Processo p = processoAccessApplicationService.load(processoId);

        NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
        authorizationService.requireReadProcessoAtSecrecy(p, efetivo);
        authorizationService.requireReadDocumentoAtSecrecy(p, d, efetivo);

        var resolved = contentService.resolvePdf(d);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento_" + documentoId + ".pdf\"");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Robots-Tag", "noindex, nofollow, noarchive");
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
