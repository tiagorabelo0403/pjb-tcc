package com.tcc.pjb.backend.tribunal.regras.plugin;

import java.time.Instant;
import java.util.Map;


    public record ResumoPlugins(
            long totalRegistrados,
            long ativos,
            long comErro,
            Map<String, Long> ativosPorTribunal,
            Instant geradoEm
    ) {}
