package com.tcc.pjb.backend.core.digitalizacao;

public record DocumentIntelligenceRequest(
        byte[] conteudo,
        String mimeType,
        String nomeArquivo,
        String idioma
) {
    public DocumentIntelligenceRequest {
        mimeType = mimeType == null ? "application/pdf" : mimeType;
        idioma = idioma == null ? "por" : idioma;
    }
}
