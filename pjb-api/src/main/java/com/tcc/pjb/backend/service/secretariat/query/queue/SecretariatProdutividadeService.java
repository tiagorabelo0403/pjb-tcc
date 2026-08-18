package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatProdutividadeItemResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatProdutividadeService {

    private static final int LIMITE_ITENS = 1000;

    private final WorkItemRepository workItemRepository;
    private final SecretariatInstitutionalVisibilityService visibilityService;

    public SecretariatProdutividadeService(WorkItemRepository workItemRepository,
                                           SecretariatInstitutionalVisibilityService visibilityService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.visibilityService = Objects.requireNonNull(visibilityService);
    }

    @Transactional(readOnly = true)
    public SecretariatProdutividadePainelResponse painelPorInbox(String inboxKey, int diasJanela) {
        String normalized = visibilityService.requireInboxAccess(inboxKey);
        Instant from = Instant.now().minus(Math.max(1, diasJanela), ChronoUnit.DAYS);
        List<WorkItem> concluidos = workItemRepository.findConcluidosPorInboxAposData(
                normalized, from, PageRequest.of(0, LIMITE_ITENS));

        Map<Long, Acumulador> porServidor = new LinkedHashMap<>();
        for (WorkItem item : concluidos) {
            Usuario servidor = item.getAssignedUser();
            if (servidor == null || servidor.getId() == null) {
                continue;
            }
            Acumulador acumulador = porServidor.computeIfAbsent(servidor.getId(), id -> new Acumulador(servidor.getNome()));
            acumulador.total++;
            if (item.getCreatedAt() != null && item.getUpdatedAt() != null && !item.getUpdatedAt().isBefore(item.getCreatedAt())) {
                acumulador.somaSegundos += Duration.between(item.getCreatedAt(), item.getUpdatedAt()).getSeconds();
                acumulador.amostras++;
            }
        }

        List<SecretariatProdutividadeItemResponse> ranking = porServidor.entrySet().stream()
                .map(entry -> new SecretariatProdutividadeItemResponse(
                        entry.getKey(),
                        entry.getValue().nome,
                        entry.getValue().total,
                        entry.getValue().amostras == 0 ? null : (entry.getValue().somaSegundos / entry.getValue().amostras) / 3600.0))
                .sorted((a, b) -> Long.compare(b.totalConcluidos(), a.totalConcluidos()))
                .toList();

        return new SecretariatProdutividadePainelResponse(normalized, diasJanela, concluidos.size(), ranking);
    }

    private static final class Acumulador {
        private final String nome;
        private long total;
        private long somaSegundos;
        private long amostras;

        private Acumulador(String nome) {
            this.nome = nome;
        }
    }
}
