package com.tcc.pjb.backend.service.publico;

import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicDocumentoDownloadService {

    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final DocumentContentService contentService;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> baixarPdf(UUID documentoId) {
        DocumentoProcessual d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));

        Long processoId = d.getProcesso() != null ? d.getProcesso().getId() : null;
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Documento", documentoId);
        }

        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        NivelSigilo sigiloProc = p.getNivelSigilo() != null ? p.getNivelSigilo() : NivelSigilo.PUBLICO;
        NivelSigilo sigiloDoc = d.getNivelSigilo() != null ? d.getNivelSigilo() : NivelSigilo.PUBLICO;

        DocumentoCategoria categoria = d.getCategoria() != null ? d.getCategoria() : DocumentoCategoria.PUBLICO;
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || !usuario.isAtivo()) {
            throw new RecursoNaoEncontradoException("Documento", documentoId);
        }
        boolean professionalMode = usuario.isAdvogado() || usuario.isMagistrado() || usuario.isServidorJudiciario() || usuario.isMinisterioPublico() || usuario.isDefensoriaPublica() || usuario.isAdminForum();
        if (!professionalMode) {
            throw new RecursoNaoEncontradoException("Documento", documentoId);
        }
        if (sigiloProc.exigeCredencial() || sigiloDoc.exigeCredencial() || categoria != DocumentoCategoria.PUBLICO) {
            authorizationService.requireReadDocumento(p, d);
        }

        var resolved = contentService.resolvePdf(d);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(safeFilename(d.getNomeOriginal()))
                .build());
        if (resolved.contentLength() > 0 && resolved.contentLength() < Integer.MAX_VALUE) {
            headers.setContentLength(resolved.contentLength());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(resolved.resource());
    }

    private static String safeFilename(String nome) {
        if (nome == null || nome.isBlank()) return "documento.pdf";
        String n = nome.replaceAll("[\\r\\n]", " ").trim();
        if (!n.toLowerCase().endsWith(".pdf")) n = n + ".pdf";
        return n;
    }
}
