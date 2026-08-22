package com.tcc.pjb.backend.model.dto.mni;

import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoPendenteClassificacaoDto(UUID id, Long processoId, String nomeOriginal, String titulo,
                                                  String sha256, LocalDateTime criadoEm) {

    public static DocumentoPendenteClassificacaoDto from(DocumentoProcessual documento) {
        return new DocumentoPendenteClassificacaoDto(
                documento.getId(),
                documento.getProcesso() == null ? null : documento.getProcesso().getId(),
                documento.getNomeOriginal(),
                documento.getDocumentoTitulo(),
                documento.getSha256(),
                documento.getCriadoEm());
    }
}
