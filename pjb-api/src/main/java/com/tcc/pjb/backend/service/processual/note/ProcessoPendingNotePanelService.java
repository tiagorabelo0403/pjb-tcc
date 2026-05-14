package com.tcc.pjb.backend.service.processual.note;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPendingNotePanelService {

    public record NotaPendente(
            Long noteId,
            Long processoId,
            ProcessoNoteType tipo,
            String body,
            Instant dueAt,
            Integer priority
    ) {}

    public record PainelNotas(
            List<NotaPendente> vencendo,
            List<NotaPendente> atrasadas,
            Map<ProcessoNoteType, Long> contagemPorTipo,
            int total
    ) {}

    public PainelNotas construir(List<NotaPendente> notas) {
        Instant agora = Instant.now();

        List<NotaPendente> atrasadas = notas.stream()
                .filter(n -> n.dueAt() != null && n.dueAt().isBefore(agora))
                .sorted(Comparator.comparing(NotaPendente::dueAt))
                .toList();

        List<NotaPendente> vencendo = notas.stream()
                .filter(n -> n.dueAt() != null && !n.dueAt().isBefore(agora)
                        && n.dueAt().isBefore(agora.plusSeconds(86400 * 3)))
                .sorted(Comparator.comparing(NotaPendente::dueAt))
                .toList();

        Map<ProcessoNoteType, Long> contagem = notas.stream()
                .filter(n -> n.tipo() != null)
                .collect(Collectors.groupingBy(NotaPendente::tipo, Collectors.counting()));

        return new PainelNotas(vencendo, atrasadas, contagem, notas.size());
    }
}
