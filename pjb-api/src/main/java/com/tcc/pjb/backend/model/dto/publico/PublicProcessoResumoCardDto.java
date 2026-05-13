package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.List;

public record PublicProcessoResumoCardDto(
        Long processoId,
        String numero,
        String tribunal,
        String uf,
        String comarca,
        String forum,
        String tipoJustica,
        String ramoDireito,
        String classeProcessual,
        String assunto,
        LocalDateTime dataDistribuicao,
        LocalDateTime dataUltimaMovimentacao,
        SigiloUiDTO sigilo,
        boolean acessoRestrito,
        String resumoPublico,
        List<PublicMovimentacaoDTO> ultimasMovimentacoes,
        String orientacaoAcesso
) {
}
