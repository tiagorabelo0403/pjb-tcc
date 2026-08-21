package com.tcc.pjb.backend.service.secretariat.acceleration;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class SecretariatQueuePriorityPolicy {

    public SecretariatQueuePriorityPolicy() {}

    public static final String TAG_REU_PRESO = "REU_PRESO";
    public static final String TAG_TUTELA = "TUTELA_URGENCIA";
    public static final String TAG_SIGILO = "SIGILO_CRITICO";
    public static final String TAG_PRIORIDADE_LEGAL = "PRIORIDADE_LEGAL";
    public static final String TAG_RISCO_NULIDADE = "RISCO_NULIDADE";

    public List<SecretariatQueuePanelItemDto> ordenar(List<SecretariatQueuePanelItemDto> itens) {
        return itens.stream()
                .sorted(Comparator
                        .comparingInt(SecretariatQueuePriorityPolicy::pontuacao).reversed()
                        .thenComparing(i -> i.dueAt() != null ? i.dueAt() : Instant.MAX))
                .toList();
    }

    private static int pontuacao(SecretariatQueuePanelItemDto item) {
        List<String> tags = item.tags() != null ? item.tags() : List.of();
        int score = 0;
        if (tags.contains(TAG_REU_PRESO)) score += 100;
        if (tags.contains(TAG_SIGILO)) score += 80;
        if (tags.contains(TAG_TUTELA)) score += 60;
        if (tags.contains(TAG_PRIORIDADE_LEGAL)) score += 40;
        if (tags.contains(TAG_RISCO_NULIDADE)) score += 30;
        if (item.dueAt() != null && item.dueAt().isBefore(Instant.now().plusSeconds(86400))) score += 20;
        if (item.prioridade() != null) score += item.prioridade();
        return score;
    }
}
