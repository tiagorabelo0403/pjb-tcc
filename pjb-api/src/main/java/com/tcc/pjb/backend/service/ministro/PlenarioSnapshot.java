package com.tcc.pjb.backend.service.ministro;

import java.time.LocalDateTime;
import java.util.List;

public record PlenarioSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String corte,
        String orgaoJulgador,
        List<String> recursosTurmaPlenario,
        List<String> questoesOrdensExpediente,
        List<String> monocraticosPendentes,
        List<String> embargosDeclaracaoPendentes,
        int adesPlenario,
        int recursosUrgentes,
        boolean stepUpRequerido,
        List<?> prazoRadar,
        Object sessionRisk
) {}
