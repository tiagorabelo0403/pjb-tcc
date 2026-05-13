package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.List;

public record PublicProcessoConsultaResponse(
        Long processoId,
        String numero,
        String tipoJustica,
        String ramoDireito,
        String classeProcessual,
        String assunto,
        LocalDateTime dataUltimaMovimentacao,

        SigiloUiDTO sigilo,

        boolean acessoRestrito,
        String aviso,
        String orientacaoAcesso,

        PublicPartesDTO partes,
        List<PublicMovimentacaoDTO> movimentacoes,
        List<PublicDocumentoDTO> documentos
) {
}
