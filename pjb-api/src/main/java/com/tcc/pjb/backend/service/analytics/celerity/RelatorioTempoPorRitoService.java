package com.tcc.pjb.backend.service.analytics.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RelatorioTempoPorRitoService {

    public record TempoPorRito(
            RitoProcessual rito,
            Duration tempoMedioConclusao,
            Duration slaLegal,
            boolean dentroDoSla,
            long quantidadeProcessos
    ) {}

    public record RelatorioTempo(
            UUID orgaoId,
            String periodo,
            List<TempoPorRito> tempos,
            String observacao
    ) {}

    public RelatorioTempo gerar(UUID orgaoId, String periodo,
            Map<RitoProcessual, Duration> temposPorRito,
            Map<RitoProcessual, Duration> slasPorRito,
            Map<RitoProcessual, Long> contagemPorRito) {
        List<TempoPorRito> tempos = temposPorRito.entrySet().stream()
                .map(e -> {
                    Duration sla = slasPorRito.getOrDefault(e.getKey(), Duration.ofDays(365));
                    long qtd = contagemPorRito.getOrDefault(e.getKey(), 0L);
                    return new TempoPorRito(e.getKey(), e.getValue(), sla,
                            e.getValue().compareTo(sla) <= 0, qtd);
                })
                .sorted((a, b) -> b.tempoMedioConclusao().compareTo(a.tempoMedioConclusao()))
                .toList();
        return new RelatorioTempo(orgaoId, periodo, tempos,
                "Tempos médios por rito — foco em gargalos estruturais, não individuais.");
    }
}
