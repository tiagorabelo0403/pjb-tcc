package com.tcc.pjb.backend.service.processual.sobrestamento;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DessobrestamentoRadarService {

    public record AlertaDessobrestamento(
            UUID processoId,
            String numeroProcesso,
            String motivoCessado,
            String acaoImediata,
            boolean urgente
    ) {}

    private final SobrestamentoInteligenteService sobrestamentoService;

    public DessobrestamentoRadarService(SobrestamentoInteligenteService sobrestamentoService) {
        this.sobrestamentoService = sobrestamentoService;
    }

    public List<AlertaDessobrestamento> detectar(
            List<SobrestamentoInteligenteService.SobrestamentoInput> processos) {
        return sobrestamentoService.varrer(processos).stream()
                .filter(SobrestamentoInteligenteService.SobrestamentoStatus::motivoCessou)
                .map(s -> {
                    SobrestamentoInteligenteService.SobrestamentoInput src = processos.stream()
                            .filter(p -> p.processoId().equals(s.processoId())).findFirst().orElseThrow();
                    return new AlertaDessobrestamento(s.processoId(), src.numeroProcesso(),
                            src.motivoSobrestamento(), s.acaoSugerida(), true);
                })
                .sorted(Comparator.comparing(AlertaDessobrestamento::urgente).reversed())
                .toList();
    }
}
