package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GigsActivityService {

    public record AtividadeGigs(
            UUID atividadeId,
            UUID processoId,
            GigsActivityType tipo,
            String descricao,
            String responsavelId,
            Instant criadaEm,
            Instant dueAt,
            boolean concluida,
            Instant concluidaEm
    ) {}

    public record AtividadesPorProcesso(
            UUID processoId,
            List<AtividadeGigs> pendentes,
            List<AtividadeGigs> concluidas,
            int totalAtrasadas
    ) {}

    public AtividadesPorProcesso agrupar(UUID processoId, List<AtividadeGigs> atividades) {
        Instant agora = Instant.now();
        List<AtividadeGigs> pendentes = atividades.stream()
                .filter(a -> !a.concluida()).toList();
        List<AtividadeGigs> concluidas = atividades.stream()
                .filter(AtividadeGigs::concluida).toList();
        int atrasadas = (int) pendentes.stream()
                .filter(a -> a.dueAt() != null && a.dueAt().isBefore(agora)).count();
        return new AtividadesPorProcesso(processoId, pendentes, concluidas, atrasadas);
    }
}
