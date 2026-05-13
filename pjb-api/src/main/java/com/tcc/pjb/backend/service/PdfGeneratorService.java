package com.tcc.pjb.backend.service;

import com.tcc.pjb.backend.core.crypto.Sha256;
import com.tcc.pjb.backend.model.dto.PdfGenerationResult;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PdfGeneratorService {

    public PdfGenerationResult generatePdfWithSealAndQr(String html, UUID u, Map<String, String> meta) {
        UUID documentId = Objects.requireNonNull(u, "Identificador do PDF é obrigatório");
        String normalizedHtml = html == null ? "" : html.trim();
        String metadataSeed = canonicalMetadata(meta);
        String hash = "SHA256-" + Sha256.hex(normalizedHtml + "|" + metadataSeed);
        String url = "/storage/pdfs/acordo_com_brasao_" + documentId + ".pdf";
        return new PdfGenerationResult(url, hash);
    }

    private String canonicalMetadata(Map<String, String> meta) {
        if (meta == null || meta.isEmpty()) {
            return "";
        }
        return new TreeMap<>(meta).entrySet().stream()
                .map(entry -> normalize(entry.getKey()) + "=" + normalize(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
