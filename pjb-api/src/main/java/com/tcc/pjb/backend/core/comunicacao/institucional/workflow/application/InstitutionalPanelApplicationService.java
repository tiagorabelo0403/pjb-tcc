package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOrgPanelSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalUnitQueueSummary;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPanelApplicationService {

    private static final Duration FILTER_CACHE_TTL = Duration.ofSeconds(12);
    private static final int MAX_FILTER_CACHE = 64;
    private static final String GLOBAL_KEY = "*";

    private final InstitutionalInboxStateRepository inboxRepository;
    private final ConcurrentHashMap<String, CachedInboxSlice> filterCache = new ConcurrentHashMap<>();

    public InstitutionalPanelApplicationService(InstitutionalInboxStateRepository inboxRepository) {
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
    }

    public List<InstitutionalOrgPanelSummary> painelOrgao(String unidadeCodigo) {
        Instant now = Instant.now();
        return filter(unidadeCodigo).stream()
                .collect(Collectors.groupingBy(item -> InstitutionalTopologyKeys.unitRecipientKey(item.unidadeCodigo(), item.destinatarioKind())))
                .values().stream()
                .map(items -> {
                    InstitutionalInboxItem first = items.get(0);
                    long atrasados = items.stream().filter(item -> isAtrasada(item, now)).count();
                    return new InstitutionalOrgPanelSummary(
                            first.unidadeCodigo(),
                            first.unidadeSigla(),
                            first.destinatarioKind().name(),
                            items.size(),
                            count(items, StatusComunicacaoInstitucional.DISPONIBILIZADA),
                            count(items, StatusComunicacaoInstitucional.RECEBIDA),
                            items.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.CIENTIFICADA || item.status() == StatusComunicacaoInstitucional.RECEBIDA || item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA).count(),
                            atrasados,
                            items.stream().map(InstitutionalInboxItem::caixaCodigoAtual).distinct().sorted().toList(),
                            now
                    );
                })
                .sorted(Comparator.comparing(InstitutionalOrgPanelSummary::unidadeSigla))
                .toList();
    }

    public List<InstitutionalUnitQueueSummary> filasUnidade(String unidadeCodigo) {
        Instant now = Instant.now();
        return filter(unidadeCodigo).stream()
                .collect(Collectors.groupingBy(item -> InstitutionalTopologyKeys.queueKey(item.unidadeCodigo(), item.caixaCodigoAtual())))
                .values().stream()
                .map(items -> {
                    InstitutionalInboxItem first = items.get(0);
                    Instant prazoMaisProximo = items.stream()
                            .map(InstitutionalInboxItem::prazoRespostaEm)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    return new InstitutionalUnitQueueSummary(
                            first.unidadeCodigo(),
                            first.unidadeSigla(),
                            first.caixaCodigoAtual(),
                            items.size(),
                            count(items, StatusComunicacaoInstitucional.DISPONIBILIZADA),
                            count(items, StatusComunicacaoInstitucional.RECEBIDA),
                            count(items, StatusComunicacaoInstitucional.CIENTIFICADA),
                            count(items, StatusComunicacaoInstitucional.CUMPRIDA),
                            items.stream().filter(item -> isAtrasada(item, now)).count(),
                            prazoMaisProximo,
                            now
                    );
                })
                .sorted(Comparator.comparing(InstitutionalUnitQueueSummary::unidadeSigla).thenComparing(InstitutionalUnitQueueSummary::caixaCodigo))
                .toList();
    }

    private List<InstitutionalInboxItem> filter(String unidadeCodigo) {
        String key = normalizeKey(unidadeCodigo);
        Instant now = Instant.now();
        CachedInboxSlice cached = filterCache.get(key);
        if (isFresh(cached, now)) {
            filterCache.put(key, new CachedInboxSlice(cached.items(), cached.expiresAt(), now));
            return cached.items();
        }
        List<InstitutionalInboxItem> loaded = GLOBAL_KEY.equals(key)
                ? inboxRepository.findAll()
                : inboxRepository.findByUnidadeCodigo(key);
        List<InstitutionalInboxItem> snapshot = List.copyOf(loaded);
        filterCache.put(key, new CachedInboxSlice(snapshot, now.plus(FILTER_CACHE_TTL), now));
        trimFilterCache(now);
        return snapshot;
    }

    private String normalizeKey(String unidadeCodigo) {
        if (unidadeCodigo == null || unidadeCodigo.isBlank()) {
            return GLOBAL_KEY;
        }
        return unidadeCodigo.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isFresh(CachedInboxSlice cached, Instant now) {
        return cached != null && cached.expiresAt() != null && cached.expiresAt().isAfter(now);
    }

    private void trimFilterCache(Instant now) {
        filterCache.entrySet().removeIf(entry -> !isFresh(entry.getValue(), now));
        if (filterCache.size() <= MAX_FILTER_CACHE) {
            return;
        }
        int removeCount = filterCache.size() - MAX_FILTER_CACHE;
        filterCache.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().touchedAt()))
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(filterCache::remove);
    }

    private long count(List<InstitutionalInboxItem> items, StatusComunicacaoInstitucional status) {
        return items.stream().filter(item -> item.status() == status).count();
    }

    private boolean isAtrasada(InstitutionalInboxItem item, Instant now) {
        return item.status() != StatusComunicacaoInstitucional.CUMPRIDA
                && item.prazoRespostaEm() != null
                && item.prazoRespostaEm().isBefore(now);
    }

    private record CachedInboxSlice(List<InstitutionalInboxItem> items, Instant expiresAt, Instant touchedAt) {
    }
}
