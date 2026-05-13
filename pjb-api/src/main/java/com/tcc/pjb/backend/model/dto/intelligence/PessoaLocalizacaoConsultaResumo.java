package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;

public record PessoaLocalizacaoConsultaResumo(
        String correlationId,
        Instant createdAt,
        PessoaLocalizacaoService.CanalConsulta canal,
        PessoaLocalizacaoFundamento fundamento,
        String referenciaProcedimental,
        String finalidade,
        String nivelExposicao,
        String posturaNivel,
        int posturaScore,
        boolean requerRevisao,
        boolean possuiContextoFormal,
        boolean enderecoEstritoLiberado,
        int fontesConsultadas,
        int enderecosEncontrados,
        int restricoesEncontradas,
        int vinculosEncontrados
) {
}
