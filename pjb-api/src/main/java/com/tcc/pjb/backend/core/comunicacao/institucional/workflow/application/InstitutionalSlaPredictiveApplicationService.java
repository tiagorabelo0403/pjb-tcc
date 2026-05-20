package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalSlaPredictiveAlert;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalSlaPredictiveDashboard;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import jakarta.inject.Inject;

@Service
public class InstitutionalSlaPredictiveApplicationService {

    private static final Duration DASHBOARD_CACHE_TTL = Duration.ofSeconds(20);

    private final InstitutionalInboxStateRepository inboxRepository;
    private final CatalogoInstitucionalUnificadoService catalogo;
    private final Clock clock;
    private final AtomicReference<CachedDashboard> dashboardCache = new AtomicReference<>();

    @Inject
    public InstitutionalSlaPredictiveApplicationService(InstitutionalInboxStateRepository inboxRepository,
                                                        CatalogoInstitucionalUnificadoService catalogo) {
        this(inboxRepository, catalogo, Clock.systemUTC());
    }

    InstitutionalSlaPredictiveApplicationService(InstitutionalInboxStateRepository inboxRepository,
                                                 CatalogoInstitucionalUnificadoService catalogo,
                                                 Clock clock) {
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public InstitutionalSlaPredictiveDashboard dashboard(String uf, DestinatarioInstitucionalKind destinatarioKind) {
        String cacheKey = cacheKey(uf, destinatarioKind);
        CachedDashboard cached = dashboardCache.get();
        if (isFresh(cached, cacheKey)) {
            return cached.dashboard();
        }
        Instant now = clock.instant();
        Map<String, UnidadeInstitucional> units = new LinkedHashMap<>();
        for (UnidadeInstitucional unit : catalogo.listarPorTipo(destinatarioKind)) {
            if (uf == null || uf.isBlank() || uf.equalsIgnoreCase(unit.uf())) {
                units.put(unit.codigo(), unit);
            }
        }
        List<InstitutionalInboxItem> inbox = units.isEmpty()
                ? List.of()
                : inboxRepository.findByUnidadeCodigos(units.keySet()).stream()
                .filter(item -> destinatarioKind == null || item.destinatarioKind() == destinatarioKind)
                .filter(item -> uf == null || uf.isBlank() || resolveUf(item, units).equalsIgnoreCase(uf))
                .toList();
        Map<String, List<InstitutionalInboxItem>> grouped = new LinkedHashMap<>();
        for (InstitutionalInboxItem item : inbox) {
            grouped.computeIfAbsent(item.unidadeCodigo(), ignored -> new ArrayList<>()).add(item);
        }
        List<InstitutionalSlaPredictiveAlert> alerts = grouped.entrySet().stream()
                .map(entry -> buildAlert(entry.getKey(), units.get(entry.getKey()), entry.getValue(), now))
                .sorted(Comparator.comparingInt(this::severityRank).thenComparing(InstitutionalSlaPredictiveAlert::horasRestantesMinimas))
                .toList();
        long criticos = alerts.stream().filter(item -> "CRITICO".equals(item.risco())).count();
        long altos = alerts.stream().filter(item -> "ALTO".equals(item.risco())).count();
        long medios = alerts.stream().filter(item -> "MEDIO".equals(item.risco())).count();
        long baixos = alerts.stream().filter(item -> "BAIXO".equals(item.risco())).count();
        InstitutionalSlaPredictiveDashboard dashboard = new InstitutionalSlaPredictiveDashboard(alerts, alerts.size(), criticos, altos, medios, baixos, now);
        dashboardCache.set(new CachedDashboard(cacheKey, dashboard, now.plus(DASHBOARD_CACHE_TTL)));
        return dashboard;
    }

    private InstitutionalSlaPredictiveAlert buildAlert(String unidadeCodigo,
                                                       UnidadeInstitucional unidade,
                                                       List<InstitutionalInboxItem> items,
                                                       Instant now) {
        long pendenciasCiencia = items.stream().filter(item -> item.cientificadaEm() == null).count();
        long pendenciasCumprimento = items.stream().filter(item -> item.status() != StatusComunicacaoInstitucional.CUMPRIDA).count();
        double mediaHistorica = items.stream()
                .filter(item -> item.cientificadaEm() != null && item.cumpridaEm() != null)
                .mapToDouble(item -> Duration.between(item.cientificadaEm(), item.cumpridaEm()).toMinutes() / 60d)
                .average()
                .orElse(24d);
        long horasRestantes = items.stream()
                .filter(item -> item.status() != StatusComunicacaoInstitucional.CUMPRIDA)
                .mapToLong(item -> minHoursRemaining(item, now))
                .min()
                .orElse(999L);
        String risco = classifyRisk(horasRestantes, pendenciasCiencia, pendenciasCumprimento, mediaHistorica);
        String mensagem = switch (risco) {
            case "CRITICO" -> "Risco crítico de violação de SLA. Redistribuição institucional imediata recomendada.";
            case "ALTO" -> "Risco alto de violação de SLA nas próximas horas. Priorizar triagem e ciência.";
            case "MEDIO" -> "Risco moderado. Monitorar carga e eventual redistribuição preventiva.";
            default -> "Carga controlada dentro do comportamento histórico da unidade.";
        };
        return new InstitutionalSlaPredictiveAlert(
                unidadeCodigo,
                unidade == null ? unidadeCodigo : unidade.sigla(),
                unidade == null ? items.getFirst().destinatarioKind() : unidade.destinatarioKind(),
                unidade == null ? null : unidade.uf(),
                pendenciasCiencia,
                pendenciasCumprimento,
                mediaHistorica,
                horasRestantes,
                risco,
                mensagem,
                now
        );
    }

    private String resolveUf(InstitutionalInboxItem item, Map<String, UnidadeInstitucional> units) {
        UnidadeInstitucional unit = units.get(item.unidadeCodigo());
        return unit == null || unit.uf() == null ? "NACIONAL" : unit.uf();
    }

    private boolean isFresh(CachedDashboard cache, String cacheKey) {
        return cache != null && Objects.equals(cache.cacheKey(), cacheKey) && cache.expiresAt() != null && cache.expiresAt().isAfter(clock.instant());
    }

    private String cacheKey(String uf, DestinatarioInstitucionalKind destinatarioKind) {
        String normalizedUf = uf == null || uf.isBlank() ? "GLOBAL" : uf.trim().toUpperCase(Locale.ROOT);
        String normalizedKind = destinatarioKind == null ? "ALL" : destinatarioKind.name();
        return normalizedUf + '|' + normalizedKind;
    }

    private long minHoursRemaining(InstitutionalInboxItem item, Instant now) {
        long ciencia = item.prazoCienciaEm() == null ? Long.MAX_VALUE : Duration.between(now, item.prazoCienciaEm()).toHours();
        long resposta = item.prazoRespostaEm() == null ? Long.MAX_VALUE : Duration.between(now, item.prazoRespostaEm()).toHours();
        return Math.min(ciencia, resposta);
    }

    private String classifyRisk(long horasRestantes, long pendenciasCiencia, long pendenciasCumprimento, double mediaHistorica) {
        if (horasRestantes < 0 || pendenciasCiencia >= 5) {
            return "CRITICO";
        }
        if (horasRestantes <= 6 || pendenciasCumprimento >= Math.max(4, Math.round(mediaHistorica / 6d))) {
            return "ALTO";
        }
        if (horasRestantes <= 24 || pendenciasCumprimento >= 2) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private int severityRank(InstitutionalSlaPredictiveAlert alert) {
        return switch (alert.risco().toUpperCase(Locale.ROOT)) {
            case "CRITICO" -> 0;
            case "ALTO" -> 1;
            case "MEDIO" -> 2;
            default -> 3;
        };
    }

    private record CachedDashboard(String cacheKey, InstitutionalSlaPredictiveDashboard dashboard, Instant expiresAt) {
    }
}
