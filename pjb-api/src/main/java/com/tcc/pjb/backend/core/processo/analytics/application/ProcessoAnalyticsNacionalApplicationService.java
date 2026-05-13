package com.tcc.pjb.backend.core.processo.analytics.application;

import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsFila;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsUnidade;
import com.tcc.pjb.backend.core.processo.busca.application.ProcessoBuscaAnalyticsApplicationService;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAnalyticsNacionalApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoBuscaAnalyticsApplicationService processoBuscaAnalyticsApplicationService;

    public ProcessoAnalyticsNacionalApplicationService(ProcessoRepository processoRepository,
                                                       ProcessoBuscaAnalyticsApplicationService processoBuscaAnalyticsApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoBuscaAnalyticsApplicationService = Objects.requireNonNull(processoBuscaAnalyticsApplicationService);
    }

    public ProcessoAnalyticsNacionalAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoAnalyticsAggregate baseline = processoBuscaAnalyticsApplicationService.analytics(
                safeName(processo.getRamoDireito()),
                firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado()),
                processo.getUf(),
                processo.getComarca()
        );
        if (baseline == null) {
            baseline = new ProcessoAnalyticsAggregate(Map.of(), 0L, 0L, 0d, 0d, 0d, 0d, List.of(), List.of(), Instant.now());
        }
        List<Processo> amostra = processoRepository.findAll(PageRequest.of(0, 600, Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao", "id"))).getContent().stream()
                .filter(item -> matchesScope(item, processo))
                .toList();

        double taxaCongestionamento = percentage(amostra.stream().filter(this::isCongestionado).count(), amostra.size());
        double taxaRetrabalho = percentage(amostra.stream().filter(this::isRetrabalho).count(), amostra.size());
        long mapaUrgencia = amostra.stream().filter(this::isUrgente).count();
        double riscoSlaGlobal = percentage(amostra.stream().filter(this::isRiscoSla).count(), amostra.size());

        List<ProcessoAnalyticsUnidade> unidadesCriticas = amostra.stream()
                .collect(Collectors.groupingBy(item -> blank(item.getVara()) ? "SEM_UNIDADE" : item.getVara(), LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toUnidade(entry.getKey(), entry.getValue()))
                .sorted(java.util.Comparator.comparing(ProcessoAnalyticsUnidade::riscoSla).reversed()
                        .thenComparing(ProcessoAnalyticsUnidade::tempoMedioDias, java.util.Comparator.reverseOrder()))
                .limit(8)
                .toList();

        List<ProcessoAnalyticsFila> gargalosFila = amostra.stream()
                .collect(Collectors.groupingBy(this::fila, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toFila(entry.getKey(), entry.getValue()))
                .sorted(java.util.Comparator.comparing(ProcessoAnalyticsFila::taxaCongestionamento).reversed()
                        .thenComparing(ProcessoAnalyticsFila::taxaRetrabalho, java.util.Comparator.reverseOrder()))
                .limit(8)
                .toList();

        LinkedHashSet<String> alertas = new LinkedHashSet<>(baseline.alertas());
        if (amostra.isEmpty()) {
            alertas.add("O recorte nacional derivado do processo não retornou amostra suficiente.");
        }
        if (riscoSlaGlobal > 30d) {
            alertas.add("Risco de SLA elevado no recorte territorial e temático do processo.");
        }
        if (taxaCongestionamento > 40d) {
            alertas.add("Congestionamento acima do ideal para a carteira observada.");
        }
        Map<String, String> recorte = new LinkedHashMap<>(baseline.escopo());
        recorte.put("processoId", String.valueOf(processoId));
        recorte.put("unidadeBase", blank(processo.getVara()) ? "SEM_UNIDADE" : processo.getVara());
        recorte.put("tribunalBase", blank(processo.getTribunal()) ? "SEM_TRIBUNAL" : processo.getTribunal());
        return new ProcessoAnalyticsNacionalAggregate(
                processoId,
                recorte,
                baseline,
                round(taxaCongestionamento),
                round(taxaRetrabalho),
                mapaUrgencia,
                round(riscoSlaGlobal),
                unidadesCriticas,
                gargalosFila,
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private ProcessoAnalyticsUnidade toUnidade(String unidade, List<Processo> processos) {
        double tempoMedio = processos.stream().mapToDouble(this::daysOpen).average().orElse(0d);
        double taxaUrgencia = percentage(processos.stream().filter(this::isUrgente).count(), processos.size());
        double riscoSla = percentage(processos.stream().filter(this::isRiscoSla).count(), processos.size());
        String faixa = riscoSla > 40d ? "CRITICA" : riscoSla > 20d ? "ELEVADA" : "OBSERVAR";
        return new ProcessoAnalyticsUnidade(unidade, processos.size(), round(tempoMedio), round(taxaUrgencia), round(riscoSla), faixa);
    }

    private ProcessoAnalyticsFila toFila(String fila, List<Processo> processos) {
        double taxaCongestionamento = percentage(processos.stream().filter(this::isCongestionado).count(), processos.size());
        double taxaRetrabalho = percentage(processos.stream().filter(this::isRetrabalho).count(), processos.size());
        long urgentes = processos.stream().filter(this::isUrgente).count();
        String severidade = taxaCongestionamento > 45d || taxaRetrabalho > 25d ? "CRITICAL" : taxaCongestionamento > 25d ? "ATTENTION" : "INFO";
        return new ProcessoAnalyticsFila(fila, processos.size(), round(taxaCongestionamento), round(taxaRetrabalho), urgentes, severidade);
    }

    private boolean matchesScope(Processo item, Processo base) {
        boolean tribunalMatch = blank(base.getTribunal()) || normalize(item.getTribunal()).equals(normalize(base.getTribunal()));
        boolean comarcaMatch = blank(base.getComarca()) || normalize(item.getComarca()).equals(normalize(base.getComarca()));
        boolean ufMatch = blank(base.getUf()) || normalize(item.getUf()).equals(normalize(base.getUf()));
        boolean ramoMatch = base.getRamoDireito() == null || item.getRamoDireito() == base.getRamoDireito();
        return tribunalMatch && comarcaMatch && ufMatch && ramoMatch;
    }

    private boolean isCongestionado(Processo processo) {
        return processo.getStatusProcesso() != null
                && processo.getStatusProcesso() != StatusProcesso.ARQUIVADO
                && daysOpen(processo) > 180d;
    }

    private boolean isRetrabalho(Processo processo) {
        String normalizedStatus = safeName(processo.getStatusProcesso());
        return normalizedStatus.contains("REDISTRIB")
                || normalizedStatus.contains("RETIFIC")
                || normalizedStatus.contains("DEVOL")
                || normalize(processo.getSubmissionBlueprintStatus()).contains("REPROCESS");
    }

    private boolean isUrgente(Processo processo) {
        return normalize(processo.getAssunto()).contains("URG")
                || normalize(processo.getAssunto()).contains("LIMINAR")
                || normalize(processo.getClasseProcessual()).contains("TUTELA")
                || safeName(processo.getFaseAtual()).contains("CUSTODIA")
                || safeName(processo.getStatusProcesso()).contains("PLANTAO");
    }

    private boolean isRiscoSla(Processo processo) {
        return isUrgente(processo) ? daysOpen(processo) > 7d : daysOpen(processo) > 60d;
    }

    private String fila(Processo processo) {
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return "FILA_RECURSAL";
        }
        if (safeName(processo.getRito()).contains("EXECUCAO")) {
            return "FILA_EXECUTIVA";
        }
        if (isUrgente(processo)) {
            return "FILA_URGENTE";
        }
        return "FILA_CONHECIMENTO";
    }

    private double daysOpen(Processo processo) {
        LocalDateTime inicio = processo.getDataCriacao() != null ? processo.getDataCriacao() : processo.getDataDistribuicao();
        LocalDateTime fim = processo.getDataUltimaMovimentacao() != null ? processo.getDataUltimaMovimentacao() : LocalDateTime.now();
        if (inicio == null) {
            return 0d;
        }
        return Duration.between(inicio.atZone(ZoneId.systemDefault()).toInstant(), fim.atZone(ZoneId.systemDefault()).toInstant()).toHours() / 24d;
    }


    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private double percentage(long value, long total) {
        if (total <= 0L) {
            return 0d;
        }
        return (value * 100d) / total;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private String safeName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
