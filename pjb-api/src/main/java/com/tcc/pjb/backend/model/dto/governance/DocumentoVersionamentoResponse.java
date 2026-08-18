package com.tcc.pjb.backend.model.dto.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.EstadoVersaoDocumentoProcessual;

public record DocumentoVersionamentoResponse(
        Long processoId,
        String numeroProcesso,
        String tituloBase,
        int proximaVersao,
        String proximoTitulo,
        boolean bloqueadoParaEdicao,
        List<DocumentoVersaoView> versoes
) {
    public record DocumentoVersaoView(
            String documentoId,
            int versao,
            String titulo,
            EstadoVersaoDocumentoProcessual estado,
            String sha256,
            @Schema(description = "Data/hora de criação da versão do documento", format = "date-time",
                    example = "2026-06-01T10:00:00-03:00") String criadoEm,
            boolean custodioAtivo
    ) {
    }
}
