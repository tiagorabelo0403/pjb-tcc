package com.tcc.pjb.backend.tribunal.regras.snapshot;

import java.time.Instant;
import java.util.List;

public record RelatorioCoberturaTribunal(
        String tribunalCodigo,
        int totalRegrasNacionais,
        int regrasPersonalizadas,
        int regrasHerdadas,
        double percentualCustomizado,
        List<String> chavesPersonalizadas,
        List<String> chavesHerdadas,
        Instant geradoEm
) {}
