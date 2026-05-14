package com.tcc.pjb.backend.service.gigs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GigsMinutaReminderBridge {

    public record LembreteMinutaGigs(
            UUID atividadeId,
            String tipo,
            String mensagem,
            boolean urgente
    ) {}

    public List<LembreteMinutaGigs> construir(UUID atividadeId,
            List<GigsActivityService.AtividadeGigs> pendentes) {
        List<LembreteMinutaGigs> lembretes = new ArrayList<>();
        for (GigsActivityService.AtividadeGigs atividade : pendentes) {
            if (atividade.tipo() == GigsActivityType.MINUTAR) {
                lembretes.add(new LembreteMinutaGigs(atividadeId, "MINUTA_PENDENTE",
                        "Atividade de minuta pendente: " + atividade.descricao(), false));
            }
            if (atividade.dueAt() != null
                    && atividade.dueAt().isBefore(java.time.Instant.now().plusSeconds(86400))) {
                lembretes.add(new LembreteMinutaGigs(atividadeId, "PRAZO_VENCENDO",
                        "Atividade com prazo vencendo: " + atividade.descricao(), true));
            }
        }
        return lembretes;
    }
}
