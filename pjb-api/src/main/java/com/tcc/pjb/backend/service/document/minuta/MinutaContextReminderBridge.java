package com.tcc.pjb.backend.service.document.minuta;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MinutaContextReminderBridge {

    public record LembreteMinuta(
            UUID minutaId,
            String tipo,
            String descricao,
            boolean urgente
    ) {}

    public record BridgeInput(
            UUID minutaId,
            boolean tutelaUrgentePendente,
            boolean reuPresso,
            boolean prazoVencendo,
            List<String> riscosIdentificados
    ) {}

    public List<LembreteMinuta> construirLembretes(BridgeInput input) {
        List<LembreteMinuta> lembretes = new java.util.ArrayList<>();
        if (input.tutelaUrgentePendente()) {
            lembretes.add(new LembreteMinuta(input.minutaId(),
                    "TUTELA_URGENTE", "Há tutela de urgência pendente — abordar na minuta.", true));
        }
        if (input.reuPresso()) {
            lembretes.add(new LembreteMinuta(input.minutaId(),
                    "REU_PRESO", "Réu preso — verificar prazo de prisão preventiva na minuta.", true));
        }
        if (input.prazoVencendo()) {
            lembretes.add(new LembreteMinuta(input.minutaId(),
                    "PRAZO", "Prazo vencendo — priorizar conclusão da minuta.", false));
        }
        for (String risco : input.riscosIdentificados()) {
            lembretes.add(new LembreteMinuta(input.minutaId(), "RISCO", risco, false));
        }
        return lembretes;
    }
}
