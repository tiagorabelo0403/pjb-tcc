package com.tcc.pjb.backend.service.timeline.surface;

import java.util.Collections;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TimelineAnalyticProjectionService {

    private final WorkItemRepository workItemRepository;

    public TimelineAnalyticProjectionService(WorkItemRepository workItemRepository) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    public Map<Long, AnalyticProjection> project(Processo processo, List<MovimentacaoProcessual> movimentacoesDesc) {
        if (movimentacoesDesc == null || movimentacoesDesc.isEmpty()) {
            return Map.of();
        }
        List<MovimentacaoProcessual> asc = movimentacoesDesc.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MovimentacaoProcessual::getDataMovimentacao, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        long openBlocking = processo == null || processo.getId() == null ? 0L : workItemRepository.countOpenBlockingByProcesso(processo.getId());
        Instant minDueAt = processo == null || processo.getId() == null ? null : workItemRepository.minOpenDueAtForProcesso(processo.getId());
        HashMap<Long, AnalyticProjection> out = new HashMap<>();
        for (int i = 0; i < asc.size(); i++) {
            MovimentacaoProcessual atual = asc.get(i);
            Instant dataAtual = atual.getDataMovimentacao();
            Instant dataFecho = i + 1 < asc.size() ? asc.get(i + 1).getDataMovimentacao() : Instant.now();
            long consumido = dataAtual == null || dataFecho == null ? 0L : Math.max(0L, ChronoUnit.DAYS.between(dataAtual, dataFecho));
            long previsto = inferExpectedDays(atual, processo);
            String status = resolveStatus(previsto, consumido);
            boolean latest = i == asc.size() - 1;
            long diasParado = latest ? consumido : Math.max(0L, consumido);
            String causa = inferCause(atual, latest, openBlocking);
            Instant proximaJanela = dataAtual == null ? null : dataAtual.plus(previsto, ChronoUnit.DAYS);
            boolean bloqueio = latest && openBlocking > 0L;
            out.put(atual.getId(), new AnalyticProjection(
                    gerouPrazo(atual),
                    consumiuPrazo(atual, i + 1 < asc.size()),
                    previsto,
                    consumido,
                    status,
                    diasParado,
                    causa,
                    proximaJanela,
                    bloqueio,
                    minDueAt
            ));
        }
        return Collections.unmodifiableMap(out);
    }

    private long inferExpectedDays(MovimentacaoProcessual movimentacao, Processo processo) {
        String descricao = movimentacao.getDescricao() == null ? "" : movimentacao.getDescricao().toUpperCase();
        if (descricao.contains("AUDI")) {
            return 30L;
        }
        if (descricao.contains("INTIMA") || descricao.contains("MANIFESTA")) {
            return 5L;
        }
        if (descricao.contains("PERIC")) {
            return 20L;
        }
        if (descricao.contains("SENTEN") || descricao.contains("DECISAO") || descricao.contains("DESPACHO")) {
            return 15L;
        }
        FaseProcessual fase = movimentacao.getFasePara() != null ? movimentacao.getFasePara() : processo == null ? null : processo.getFaseAtual();
        if (fase == null) {
            return 10L;
        }
        return switch (fase) {
            case RECURSAL -> 15L;
            case AUDIENCIA_CUSTODIA, INSTRUTORIA, PERICIA_TECNICA -> 20L;
            case CONHECIMENTO -> 7L;
            default -> 10L;
        };
    }

    private boolean gerouPrazo(MovimentacaoProcessual movimentacao) {
        String descricao = movimentacao.getDescricao() == null ? "" : movimentacao.getDescricao().toUpperCase();
        return descricao.contains("INTIMA") || descricao.contains("PRAZO") || descricao.contains("VISTA") || descricao.contains("MANIFESTA");
    }

    private boolean consumiuPrazo(MovimentacaoProcessual movimentacao, boolean hasNext) {
        return hasNext && gerouPrazo(movimentacao);
    }

    private String resolveStatus(long previsto, long consumido) {
        if (previsto <= 0L) {
            return "INFORMATIVO";
        }
        if (consumido >= previsto + Math.max(2L, previsto / 3L)) {
            return "ESTOURO";
        }
        if (consumido >= Math.max(1L, previsto - Math.max(1L, previsto / 5L))) {
            return "ALERTA";
        }
        return "DENTRO_PRAZO";
    }

    private String inferCause(MovimentacaoProcessual movimentacao, boolean latest, long openBlocking) {
        String descricao = movimentacao.getDescricao() == null ? "" : movimentacao.getDescricao().toUpperCase();
        if (latest && openBlocking > 0L) {
            return "Aguardando cumprimento de work item bloqueante na trilha institucional.";
        }
        if (descricao.contains("AUDI")) {
            return "Aguardando agenda ou fechamento do ciclo de audiência.";
        }
        if (descricao.contains("PERIC")) {
            return "Aguardando prova técnica, laudo ou saneamento da trilha pericial.";
        }
        if (descricao.contains("INTIMA") || descricao.contains("MANIFESTA") || descricao.contains("VISTA")) {
            return "Aguardando manifestação da parte ou consumo do prazo aberto por intimação.";
        }
        if (descricao.contains("CONCLUS") || descricao.contains("GABINETE") || descricao.contains("DECISAO") || descricao.contains("DESPACHO")) {
            return "Aguardando impulso decisório ou revisão do gabinete.";
        }
        return "Aguardando evolução do próximo passo material da fase processual.";
    }

    public record AnalyticProjection(boolean gerouPrazo,
                                     boolean consumiuPrazo,
                                     long prazoPrevistoDias,
                                     long prazoConsumidoDias,
                                     String prazoStatus,
                                     long diasParado,
                                     String causaProvavelParada,
                                     Instant proximaJanelaTeorica,
                                     boolean bloqueioOperacional,
                                     Instant deadlineOperacionalAberto) {
    }
}
