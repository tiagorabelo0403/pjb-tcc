package com.tcc.pjb.backend.service.processual.precedente;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecedenteDecisionSupportAssembler {

    public record PrecedenteItem(
            String numeroTema,
            String tribunal,
            String ementa,
            boolean temaSuspenso,
            boolean repetitivo
    ) {}

    public record DecisionSupport(
            UUID processoId,
            RitoProcessual rito,
            List<PrecedenteItem> precedentes,
            boolean haTemaSuspenso,
            boolean haDivergencia,
            String aviso
    ) {}

    public DecisionSupport montar(UUID processoId, RitoProcessual rito,
            List<PrecedenteItem> precedentes) {
        boolean suspenso = precedentes.stream().anyMatch(PrecedenteItem::temaSuspenso);
        boolean divergencia = precedentes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PrecedenteItem::ementa, java.util.stream.Collectors.counting()))
                .values().stream().anyMatch(c -> c > 1);

        String aviso = suspenso
                ? "Há tema suspenso relacionado — verificar se processo deve ser sobrestado."
                : haDivergencia(precedentes)
                        ? "Há divergência jurisprudencial — analisar antes de decidir."
                        : "Precedentes disponíveis para análise — decisão é do magistrado.";

        return new DecisionSupport(processoId, rito, precedentes, suspenso, divergencia, aviso);
    }

    private boolean haDivergencia(List<PrecedenteItem> precedentes) {
        return precedentes.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        p -> p.tribunal(),
                        java.util.stream.Collectors.counting())).size() > 1;
    }
}
