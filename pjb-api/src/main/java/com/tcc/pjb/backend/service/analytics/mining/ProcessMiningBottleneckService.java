package com.tcc.pjb.backend.service.analytics.mining;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessMiningBottleneckService {

    public record GargaloMining(
            FaseProcessual fase,
            String tipoAto,
            Duration duracaoMedia,
            long frequencia,
            String descricao
    ) {}

    private final ProcessMiningEventLogService eventLogService;

    public ProcessMiningBottleneckService(ProcessMiningEventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    public List<GargaloMining> identificarGargalos(
            List<ProcessMiningEventLogService.EventoProcesso> eventos,
            Duration limiarGargalo) {
        return eventLogService.agregar(eventos).stream()
                .filter(e -> e.duracaoMedia().compareTo(limiarGargalo) > 0)
                .map(e -> new GargaloMining(
                        e.fase(), e.tipoAto(), e.duracaoMedia(), e.frequencia(),
                        "Fase " + e.fase().name() + " — ato '" + e.tipoAto()
                                + "' com tempo médio de " + e.duracaoMedia().toDays() + " dia(s)."))
                .sorted(Comparator.comparing(GargaloMining::duracaoMedia).reversed())
                .toList();
    }
}
