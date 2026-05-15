package com.tcc.pjb.backend.core.processo.busca.application;

import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsIndicador;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaCard;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaFacet;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsAggregationService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProcessoBuscaAnalyticsApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoAnalyticsAggregationService analyticsAggregationService;

    public ProcessoBuscaAnalyticsApplicationService(ProcessoRepository processoRepository,
                                                     ProcessoAnalyticsAggregationService analyticsAggregationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.analyticsAggregationService = Objects.requireNonNull(analyticsAggregationService);
    }

    public ProcessoBuscaAggregate buscar(String cpf,
                                         String nome,
                                         String numero,
                                         String uf,
                                         String comarca,
                                         String ramo,
                                         String status,
                                         String tribunal,
                                         int pagina,
                                         int tamanho) {
        int normalizedPage = Math.max(0, pagina);
        int normalizedSize = Math.max(1, Math.min(100, tamanho));
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao", "id"));
        Page<Processo> page = selectPage(cpf, nome, numero, pageable);
        List<Processo> filtrados = page.getContent().stream().filter(item -> matches(item, uf, comarca, ramo, status, tribunal)).toList();
        List<ProcessoBuscaCard> cards = filtrados.stream().map(this::toCard).toList();
        List<ProcessoBuscaFacet> facets = buildFacets(filtrados);
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        boolean filtragemPosPagina = requiresPostFilter(uf, comarca, ramo, status, tribunal);
        if (filtragemPosPagina) {
            alertas.add("Parte dos filtros foi aplicada após a paginação para preservar compatibilidade com a base atual.");
        }
        if (cards.isEmpty()) {
            alertas.add("Nenhum processo encontrado para a combinação informada na amostra retornada.");
        }
        return new ProcessoBuscaAggregate(
                filters(cpf, nome, numero, uf, comarca, ramo, status, tribunal),
                normalizedPage,
                normalizedSize,
                page.getTotalElements(),
                filtragemPosPagina,
                cards,
                facets,
                List.copyOf(alertas),
                Instant.now()
        );
    }

    public ProcessoAnalyticsAggregate analytics(String ramo, String tribunal, String uf, String comarca) {
        long totalProcessos = comarca != null && !comarca.isBlank() ? processoRepository.countByUfAndComarca(blankToNull(uf), comarca.trim())
                : uf != null && !uf.isBlank() ? processoRepository.countByUf(uf.trim())
                : processoRepository.count();

        AnalyticsSnapshot snapshot = analyticsSnapshot(ramo, tribunal, uf, comarca);
        ArrayList<ProcessoAnalyticsIndicador> indicadores = new ArrayList<>();
        indicadores.add(new ProcessoAnalyticsIndicador("TOTAL_PROCESSOS", "Carteira total observada", totalProcessos, "count", totalProcessos == 0 ? "ATTENTION" : "INFO", List.of(scopeLabel(ramo, tribunal, uf, comarca))));
        indicadores.add(new ProcessoAnalyticsIndicador("ATIVOS", "Processos ativos", snapshot.totalAtivos(), "count", snapshot.totalAtivos() == 0 ? "ATTENTION" : "INFO", List.of("Carteira ativa derivada do recorte disponível.")));
        indicadores.add(new ProcessoAnalyticsIndicador("TEMPO_MEDIO", "Tempo médio de tramitação", snapshot.tempoMedioDias(), "dias", snapshot.tempoMedioDias() > 365d ? "ATTENTION" : "INFO", List.of("Métrica nacional derivada do recorte ou do ramo selecionado.")));
        indicadores.add(new ProcessoAnalyticsIndicador("TAXA_RECURSAL", "Taxa recursal", snapshot.taxaRecursal(), "%", snapshot.taxaRecursal() > 35d ? "ATTENTION" : "INFO", List.of("Recursal calculado sobre o recorte processual carregado.")));
        indicadores.add(new ProcessoAnalyticsIndicador("TAXA_ACORDO", "Taxa de acordo", snapshot.taxaAcordo(), "%", snapshot.taxaAcordo() < 5d ? "ATTENTION" : "INFO", List.of("Acordos detectados por resultado final e homologação.")));
        indicadores.add(new ProcessoAnalyticsIndicador("TAXA_URGENCIA", "Taxa de urgência", snapshot.taxaUrgencia(), "%", snapshot.taxaUrgencia() > 20d ? "ATTENTION" : "INFO", List.of("Urgência calculada por assunto, classe e sinalização do processo.")));

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (totalProcessos == 0) {
            alertas.add("O recorte informado não retornou processos contabilizados.");
        }
        if (snapshot.amostragem()) {
            alertas.add("Parte dos indicadores foi calculada por amostra para manter desempenho enquanto o catálogo analítico nacional é consolidado.");
        }
        if (ramo == null || ramo.isBlank()) {
            alertas.add("Informe um ramo para obter agregados exatos por carteira temática e tribunal.");
        }
        return new ProcessoAnalyticsAggregate(
                scope(ramo, tribunal, uf, comarca),
                totalProcessos,
                snapshot.totalAtivos(),
                round(snapshot.tempoMedioDias()),
                round(snapshot.taxaRecursal()),
                round(snapshot.taxaAcordo()),
                round(snapshot.taxaUrgencia()),
                indicadores,
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private Page<Processo> selectPage(String cpf, String nome, String numero, Pageable pageable) {
        if (notBlank(cpf) || notBlank(nome) || notBlank(numero)) {
            return processoRepository.searchQuick(blankToNull(cpf), blankToNull(nome), blankToNull(numero), pageable);
        }
        return processoRepository.findAll(pageable);
    }

    private AnalyticsSnapshot analyticsSnapshot(String ramo, String tribunal, String uf, String comarca) {
        if (notBlank(ramo) && notBlank(tribunal)) {
            List<Object[]> rows = analyticsAggregationService.agregadosPorRamoETribunal(ramo.trim().toUpperCase(Locale.ROOT), tribunal.trim().toUpperCase(Locale.ROOT));
            Optional<Object[]> row = rows.stream().findFirst();
            if (row.isPresent()) {
                return fromRamoTribunal(row.get());
            }
        }
        if (notBlank(ramo)) {
            List<Object[]> rows = analyticsAggregationService.agregadosPorRamo(ramo.trim().toUpperCase(Locale.ROOT));
            Optional<Object[]> row = rows.stream().findFirst();
            if (row.isPresent()) {
                return fromRamo(row.get());
            }
        }
        List<Processo> sample = processoRepository.findAll(PageRequest.of(0, 300, Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao", "id"))).getContent().stream()
                .filter(item -> matches(item, uf, comarca, ramo, null, tribunal))
                .toList();
        long total = sample.size();
        if (total == 0) {
            return new AnalyticsSnapshot(0L, 0L, 0d, 0d, 0d, 0d, true);
        }
        long ativos = sample.stream().filter(item -> item.getStatusProcesso() != StatusProcesso.ARQUIVADO && item.getStatusProcesso() != StatusProcesso.TRANSITO_EM_JULGADO).count();
        long recursais = sample.stream().filter(item -> item.getFaseAtual() == FaseProcessual.RECURSAL || containsAny(item.getStatusProcesso() == null ? null : item.getStatusProcesso().name(), "RECURSO", "EMBARGO")).count();
        long acordos = sample.stream().filter(this::hasAcordo).count();
        long urgencias = sample.stream().filter(this::isUrgente).count();
        double tempoMedio = sample.stream().mapToDouble(this::daysOpen).average().orElse(0d);
        return new AnalyticsSnapshot(total, ativos, tempoMedio, percentage(recursais, total), percentage(acordos, total), percentage(urgencias, total), true);
    }

    private AnalyticsSnapshot fromRamo(Object[] row) {
        long total = longValue(row, 1);
        long julgados = longValue(row, 2);
        long recursais = longValue(row, 3);
        long ativos = longValue(row, 4);
        double tempo = doubleValue(row, 5);
        return new AnalyticsSnapshot(total, ativos, tempo, percentage(recursais, total), percentage(julgados, total), 0d, false);
    }

    private AnalyticsSnapshot fromRamoTribunal(Object[] row) {
        long total = longValue(row, 0);
        long ativos = longValue(row, 1);
        long acordos = longValue(row, 2);
        long urgencias = longValue(row, 3);
        return new AnalyticsSnapshot(total, ativos, 0d, 0d, percentage(acordos, total), percentage(urgencias, total), false);
    }

    private ProcessoBuscaCard toCard(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        add(marcadores, processo.getTribunal());
        add(marcadores, processo.getComarca());
        add(marcadores, safeName(processo.getRamoDireito()));
        add(marcadores, safeName(processo.getRito()));
        add(marcadores, safeName(processo.getFaseAtual()));
        add(marcadores, safeName(processo.getStatusProcesso()));
        add(marcadores, safeName(processo.getNivelSigilo()));
        return new ProcessoBuscaCard(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                processo.getVara(),
                processo.getComarca(),
                processo.getUf(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                accentColor(processo),
                isUrgente(processo),
                List.copyOf(marcadores),
                toInstant(processo.getDataUltimaMovimentacao() != null ? processo.getDataUltimaMovimentacao() : processo.getDataCriacao())
        );
    }

    private List<ProcessoBuscaFacet> buildFacets(List<Processo> processos) {
        ArrayList<ProcessoBuscaFacet> facets = new ArrayList<>();
        facets.addAll(group(processos, "RAMO", item -> safeName(item.getRamoDireito())));
        facets.addAll(group(processos, "FASE", item -> safeName(item.getFaseAtual())));
        facets.addAll(group(processos, "STATUS", item -> safeName(item.getStatusProcesso())));
        facets.addAll(group(processos, "TRIBUNAL", Processo::getTribunal));
        return List.copyOf(facets);
    }

    private List<ProcessoBuscaFacet> group(List<Processo> processos, String eixo, java.util.function.Function<Processo, String> resolver) {
        return processos.stream()
                .collect(Collectors.groupingBy(item -> normalize(resolver.apply(item)), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ProcessoBuscaFacet(eixo, entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean matches(Processo processo, String uf, String comarca, String ramo, String status, String tribunal) {
        return matchesText(processo.getUf(), uf)
                && matchesText(processo.getComarca(), comarca)
                && matchesText(safeName(processo.getRamoDireito()), ramo)
                && matchesText(safeName(processo.getStatusProcesso()), status)
                && matchesText(processo.getTribunal(), tribunal);
    }

    private boolean matchesText(String actual, String expected) {
        if (!notBlank(expected)) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return normalize(actual).contains(normalize(expected));
    }

    private boolean hasAcordo(Processo processo) {
        return containsAny(processo.getResultadoFinal(), "ACORDO", "CONCILIACAO", "CONCILIAÇÃO", "MEDIACAO", "MEDIAÇÃO", "HOMOLOG")
                || containsAny(processo.getAssunto(), "ACORDO", "CONCILIACAO", "MEDIAÇÃO");
    }

    private boolean isUrgente(Processo processo) {
        return containsAny(processo.getClasseProcessual(), "TUTELA", "CAUTELAR")
                || containsAny(processo.getAssunto(), "URG", "LIMINAR", "TUTELA", "CAUTELAR")
                || containsAny(safeName(processo.getStatusProcesso()), "URG", "PLANTAO");
    }

    private String accentColor(Processo processo) {
        if (isUrgente(processo)) {
            return "rose";
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL || containsAny(safeName(processo.getStatusProcesso()), "RECURSO", "EMBARGO")) {
            return "amber";
        }
        if (containsAny(safeName(processo.getRito()), "EXECUCAO", "EXECUÇÃO")) {
            return "red";
        }
        if (containsAny(processo.getAssunto(), "PERICIA", "PSICOSSOCIAL", "LAUDO")) {
            return "violet";
        }
        return "slate";
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Map<String, String> filters(String cpf, String nome, String numero, String uf, String comarca, String ramo, String status, String tribunal) {
        LinkedHashMap<String, String> filtros = new LinkedHashMap<>();
        put(filtros, "cpf", cpf);
        put(filtros, "nome", nome);
        put(filtros, "numero", numero);
        put(filtros, "uf", uf);
        put(filtros, "comarca", comarca);
        put(filtros, "ramo", ramo);
        put(filtros, "status", status);
        put(filtros, "tribunal", tribunal);
        return filtros;
    }

    private Map<String, String> scope(String ramo, String tribunal, String uf, String comarca) {
        LinkedHashMap<String, String> escopo = new LinkedHashMap<>();
        put(escopo, "ramo", ramo);
        put(escopo, "tribunal", tribunal);
        put(escopo, "uf", uf);
        put(escopo, "comarca", comarca);
        return escopo;
    }

    private String scopeLabel(String ramo, String tribunal, String uf, String comarca) {
        return scope(ramo, tribunal, uf, comarca).entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue()).collect(Collectors.joining(", "));
    }

    private boolean requiresPostFilter(String uf, String comarca, String ramo, String status, String tribunal) {
        return notBlank(uf) || notBlank(comarca) || notBlank(ramo) || notBlank(status) || notBlank(tribunal);
    }

    private void put(Map<String, String> target, String key, String value) {
        if (notBlank(value)) {
            target.put(key, value.trim());
        }
    }

    private void add(LinkedHashSet<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private boolean containsAny(String value, String... probes) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        for (String probe : probes) {
            if (normalize(probe).isBlank()) {
                continue;
            }
            if (normalized.contains(normalize(probe))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    private long longValue(Object[] row, int index) {
        Object value = row != null && row.length > index ? row[index] : null;
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double doubleValue(Object[] row, int index) {
        Object value = row != null && row.length > index ? row[index] : null;
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0d : (part * 100d) / total;
    }

    private double daysOpen(Processo processo) {
        LocalDateTime start = processo.getDataCriacao() != null ? processo.getDataCriacao() : processo.getDataDistribuicao();
        LocalDateTime end = processo.getDataUltimaMovimentacao() != null ? processo.getDataUltimaMovimentacao() : LocalDateTime.now();
        if (start == null) {
            return 0d;
        }
        return Math.max(0d, java.time.Duration.between(start, end).toHours() / 24d);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private record AnalyticsSnapshot(long totalProcessos,
                                     long totalAtivos,
                                     double tempoMedioDias,
                                     double taxaRecursal,
                                     double taxaAcordo,
                                     double taxaUrgencia,
                                     boolean amostragem) {
    }
}
