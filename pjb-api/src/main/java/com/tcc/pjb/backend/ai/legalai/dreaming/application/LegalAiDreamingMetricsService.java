package com.tcc.pjb.backend.ai.legalai.dreaming.application;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateType;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiDreamingMetricsService {

    public record MetricasSnapshot(
            int totalSessoes,
            Instant ultimoDreamExecutadoEm,
            int padroesConsolidados,
            double taxaReutilizacaoMemoria,
            Map<String, Integer> evolucaoPorRitoProcessual,
            Map<MemoryCandidateType, Long> memoriasPorDominio
    ) {}

    private int totalSessoes = 0;
    private Instant ultimoExecucao = null;

    public void registrarSessao() {
        totalSessoes++;
        ultimoExecucao = Instant.now();
    }

    public MetricasSnapshot snapshot() {
        Map<MemoryCandidateType, Long> memorias = new EnumMap<>(MemoryCandidateType.class);
        for (MemoryCandidateType tipo : MemoryCandidateType.values()) {
            memorias.put(tipo, 0L);
        }
        return new MetricasSnapshot(
                totalSessoes,
                ultimoExecucao,
                0,
                totalSessoes > 0 ? 0.0 : 0.0,
                Map.of(),
                memorias);
    }
}
