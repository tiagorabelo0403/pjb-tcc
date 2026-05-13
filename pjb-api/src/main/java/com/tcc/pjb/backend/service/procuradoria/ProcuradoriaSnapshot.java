package com.tcc.pjb.backend.service.procuradoria;

import java.time.LocalDateTime;
import java.util.List;

public record ProcuradoriaSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String orgao,
        String esfera,
        List<String> acoesHazendarias,
        List<String> contestacoesPendentes,
        List<String> recursosFiscais,
        List<String> pareceresPendentes,
        int prazos48h,
        List<?> prazoRadar,
        Object sessionRisk
) {}
