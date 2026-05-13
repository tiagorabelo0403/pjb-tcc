package com.tcc.pjb.backend.tribunal.regras.plugin;

import java.time.Instant;
import java.util.List;


    public record ResultadoCarga(
            String pluginKey,
            String pluginId,
            String tribunalCodigo,
            String resolucao,
            boolean sucesso,
            StatusPlugin status,
            int regrasCadastradas,
            int regrasRulePackCadastradas,
            int feriadosCadastrados,
            int calendarioEntradasCadastradas,
            int recessosCadastrados,
            int ignorados,
            List<String> erros,
            List<String> avisos,
            Instant processadoEm
    ) {}
