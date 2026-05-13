package com.tcc.pjb.backend.model.dto.governance;

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
            String criadoEm,
            boolean custodioAtivo
    ) {
    }
}
