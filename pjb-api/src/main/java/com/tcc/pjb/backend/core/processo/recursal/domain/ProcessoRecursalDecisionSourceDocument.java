package com.tcc.pjb.backend.core.processo.recursal.domain;

import java.time.Instant;
import java.util.UUID;

public record ProcessoRecursalDecisionSourceDocument(
        UUID documentoId,
        String documentType,
        String titulo,
        String nomeOriginal,
        String contentType,
        Long tamanhoBytes,
        String sha256,
        String sha384,
        String origemSistema,
        String storageBackend,
        String storageUri,
        String pdfEndpoint,
        String visualizationMode,
        Integer totalPaginas,
        Integer paginasComTexto,
        String textoIntegralExtraido,
        Instant criadoEm
) {
    public ProcessoRecursalDecisionSourceDocument {
        documentType = normalize(documentType);
        titulo = normalize(titulo);
        nomeOriginal = normalize(nomeOriginal);
        contentType = normalize(contentType);
        sha256 = normalize(sha256);
        sha384 = normalize(sha384);
        origemSistema = normalize(origemSistema);
        storageBackend = normalize(storageBackend);
        storageUri = normalize(storageUri);
        pdfEndpoint = normalize(pdfEndpoint);
        visualizationMode = normalize(visualizationMode);
        textoIntegralExtraido = normalizeText(textoIntegralExtraido);
    }

    public boolean available() {
        return documentoId != null
                || !blank(pdfEndpoint)
                || !blank(textoIntegralExtraido)
                || !blank(titulo)
                || !blank(nomeOriginal);
    }

    public boolean hasIntegralText() {
        return !blank(textoIntegralExtraido);
    }

    public boolean hasPdfView() {
        return !blank(pdfEndpoint);
    }

    public String displayTitle() {
        if (!blank(titulo)) {
            return titulo;
        }
        if (!blank(nomeOriginal)) {
            return nomeOriginal;
        }
        return documentType;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.trim().replaceAll("\\s+", " ");
        return compact.isBlank() ? null : compact;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
