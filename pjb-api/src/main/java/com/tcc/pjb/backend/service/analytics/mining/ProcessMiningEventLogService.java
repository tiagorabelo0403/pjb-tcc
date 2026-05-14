package com.tcc.pjb.backend.service.analytics.mining;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessMiningEventLogService {

    public record EventoProcesso(
            UUID processoId,
            RitoProcessual rito,
            FaseProcessual fase,
            String tipoAto,
            Instant inicio,
            Instant fim,
            String responsavelTipo
    ) {}

    public record EventoAgregado(
            FaseProcessual fase,
            String tipoAto,
            Duration duracaoMedia,
            long frequencia,
            double taxaRetrabalho
    ) {}

    public List<EventoAgregado> agregar(List<EventoProcesso> eventos) {
        return eventos.stream()
                .filter(e -> e.inicio() != null && e.fim() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.fase().name() + "|" + e.tipoAto()))
                .entrySet().stream()
                .map(entry -> {
                    List<EventoProcesso> grupo = entry.getValue();
                    Duration media = grupo.stream()
                            .map(e -> Duration.between(e.inicio(), e.fim()))
                            .reduce(Duration.ZERO, Duration::plus)
                            .dividedBy(Math.max(grupo.size(), 1));
                    String[] parts = entry.getKey().split("\\|");
                    FaseProcessual fase = FaseProcessual.valueOf(parts[0]);
                    return new EventoAgregado(fase, parts[1], media, grupo.size(), 0.0);
                })
                .sorted((a, b) -> b.duracaoMedia().compareTo(a.duracaoMedia()))
                .toList();
    }
}
