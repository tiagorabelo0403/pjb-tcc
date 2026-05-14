package com.tcc.pjb.backend.service.processual.acceleration.penal;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PenalReuPresoPriorityService {

    public record ReuPresoItem(
            UUID processoId,
            String numeroProcesso,
            Duration tempoPreso,
            boolean audienciaMarcada,
            boolean excedeuPrazo
    ) {}

    public record AlertaReuPreso(
            UUID processoId,
            String numeroProcesso,
            String alerta,
            int prioridade
    ) {}

    public List<AlertaReuPreso> identificarAlertas(List<ReuPresoItem> processos) {
        return processos.stream()
                .map(this::avaliar)
                .sorted(Comparator.comparingInt(AlertaReuPreso::prioridade).reversed())
                .toList();
    }

    private AlertaReuPreso avaliar(ReuPresoItem item) {
        if (item.excedeuPrazo()) {
            return new AlertaReuPreso(item.processoId(), item.numeroProcesso(),
                    "Réu preso com prazo excedido — constrangimento ilegal imediato.", 100);
        }
        if (!item.audienciaMarcada() && item.tempoPreso().toDays() > 30) {
            return new AlertaReuPreso(item.processoId(), item.numeroProcesso(),
                    "Réu preso há " + item.tempoPreso().toDays() + " dias sem audiência marcada.", 80);
        }
        return new AlertaReuPreso(item.processoId(), item.numeroProcesso(),
                "Réu preso — acompanhamento regular de prazo.", 40);
    }
}
