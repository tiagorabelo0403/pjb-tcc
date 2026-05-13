package com.tcc.pjb.backend.service.conciliacao;

import java.time.LocalDateTime;
import java.util.List;

public record ConciliacaoSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String cejusc,
        String modalidade,
        List<String> sessoesPendentes,
        List<String> acordosPendentesHomologacao,
        List<String> novosCasosDesignados,
        int sessoesHoje,
        int taxaAcordoPercent,
        List<?> prazoRadar,
        Object sessionRisk
) {}
