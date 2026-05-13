package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.List;

public record AdvogadoPublicoProcessoIntegralResponse(
        boolean acessoIntegralDisponivel,
        boolean requerAutorizacaoCliente,
        String orientacaoAcesso,
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
        PublicPartesDTO partes,
        List<PublicMovimentacaoDTO> movimentacoes,
        List<PublicDocumentoDTO> documentos
) {
}
