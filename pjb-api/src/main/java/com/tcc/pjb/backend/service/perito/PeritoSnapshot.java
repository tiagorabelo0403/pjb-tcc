package com.tcc.pjb.backend.service.perito;

import java.time.LocalDateTime;
import java.util.List;

public record PeritoSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String especialidade,
        List<String> nomeacoesPendentes,
        List<String> laudosPendentes,
        List<String> quesitosResponder,
        int honorariosPendentes,
        int prazosUrgentes,
        List<?> prazoRadar,
        Object sessionRisk
) {}
