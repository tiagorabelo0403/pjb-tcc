package com.tcc.pjb.backend.service.analytics.celerity;

import com.tcc.pjb.backend.service.processual.celerity.ProcessoGargaloTipo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RelatorioGargaloProcessualService {

    public record GargaloAgregado(
            ProcessoGargaloTipo tipo,
            long quantidade,
            double percentualDoTotal,
            String acaoSugerida
    ) {}

    public record RelatorioGargalo(
            UUID orgaoId,
            String periodo,
            List<GargaloAgregado> gargalos,
            String observacao
    ) {}

    public RelatorioGargalo gerar(UUID orgaoId, String periodo,
            Map<ProcessoGargaloTipo, Long> contagens) {
        long total = contagens.values().stream().mapToLong(Long::longValue).sum();
        List<GargaloAgregado> gargalos = contagens.entrySet().stream()
                .map(e -> new GargaloAgregado(
                        e.getKey(), e.getValue(),
                        total > 0 ? e.getValue() * 100.0 / total : 0,
                        "Verificar processos com " + e.getKey().name()))
                .sorted((a, b) -> Long.compare(b.quantidade(), a.quantidade()))
                .toList();
        return new RelatorioGargalo(orgaoId, periodo, gargalos,
                "Relatório de gargalos — sem identificação individual de magistrado.");
    }
}
