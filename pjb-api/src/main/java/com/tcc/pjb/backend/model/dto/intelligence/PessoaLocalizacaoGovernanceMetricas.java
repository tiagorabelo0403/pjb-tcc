package com.tcc.pjb.backend.model.dto.intelligence;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;

public record PessoaLocalizacaoGovernanceMetricas(
        PessoaLocalizacaoService.CanalConsulta canal,
        Instant janela24hInicio,
        Instant janela7dInicio,
        int consultasUltimas24h,
        int consultasUltimos7Dias,
        int consultasComRevisao,
        int consultasEnderecoEstrito,
        int consultasSemContextoFormal,
        int consultasStepUpPendentes,
        double scoreMedio,
        String posturaPredominante,
        boolean exigeAtencaoOperacional,
        List<PessoaLocalizacaoConsultaResumo> consultasRecentes,
        List<String> alertasOperacionais
) {
    public PessoaLocalizacaoGovernanceMetricas {
        consultasRecentes = consultasRecentes == null ? List.of() : List.copyOf(consultasRecentes);
        alertasOperacionais = alertasOperacionais == null ? List.of() : List.copyOf(alertasOperacionais);
    }
}
