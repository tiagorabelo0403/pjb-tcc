package com.tcc.pjb.backend.service.cidadao;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;

import lombok.RequiredArgsConstructor;





@Service
@RequiredArgsConstructor
public class CidadaoDocumentoDownloadService {

    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final DocumentContentService contentService;
    private final RecursalEffectiveSecrecyService secrecyService;

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> baixarPdf(Long processoId, UUID documentoId) {
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", "(n/a)");
        }

        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        
        authorizationService.requireReadProcessoAsCidadaoParte(p);

        
        NivelSigilo efetivoProc = secrecyService.effectiveSecrecyForProcesso(processoId);
        authorizationService.requireReadProcessoAtSecrecy(p, efetivoProc);

        DocumentoProcessual d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));

        
        Long docProcessoId = (d.getProcesso() != null) ? d.getProcesso().getId() : null;
        if (docProcessoId == null || !docProcessoId.equals(processoId)) {
            throw new RecursoNaoEncontradoException("Documento", documentoId);
        }

        authorizationService.requireReadDocumentoAtSecrecy(p, d, efetivoProc);

        var resolved = contentService.resolvePdf(d);

        NivelSigilo sigiloProc = efetivoProc != null ? efetivoProc : (p.getNivelSigilo() != null ? p.getNivelSigilo() : NivelSigilo.PUBLICO);
        NivelSigilo sigiloDoc = d.getNivelSigilo() != null ? d.getNivelSigilo() : NivelSigilo.PUBLICO;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(safeFilename(d.getNomeOriginal()))
                .build());

        if (resolved.contentLength() > 0 && resolved.contentLength() < Integer.MAX_VALUE) {
            headers.setContentLength(resolved.contentLength());
        }

        
        if (sigiloProc.exigeCredencial() || sigiloDoc.exigeCredencial()) {
            headers.setCacheControl("no-store");
            headers.setPragma("no-cache");
        }

        return ResponseEntity.ok().headers(headers).body(resolved.resource());
    }

    private static String safeFilename(String nome) {
        if (nome == null || nome.isBlank()) return "documento.pdf";
        String n = nome.replaceAll("[\\r\\n]", " ").trim();
        if (!n.toLowerCase().endsWith(".pdf")) n = n + ".pdf";
        return n;
    }
}
