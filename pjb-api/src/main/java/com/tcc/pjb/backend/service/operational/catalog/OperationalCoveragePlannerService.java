package com.tcc.pjb.backend.service.operational.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OperationalCoveragePlannerService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public OperationalCoveragePlannerService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public CoverageProjection resolveSecretariat(String inboxKey, Collection<SecretariatQueueItem> items) {
        Instant now = Instant.now();
        LinkedHashMap<String, CellAccumulator> byCell = new LinkedHashMap<>();
        if (items != null) {
            for (SecretariatQueueItem item : items) {
                Map<String, Object> metadata = parseJson(item == null ? null : item.getMetadataJson());
                String cellCode = firstNonBlank(normalize(item == null ? null : item.getDeskAxis()), normalize(item == null ? null : item.getLaneCode()), normalize(item == null ? null : item.getQueueCode()), "CELULA_GERAL");
                String cellLabel = firstNonBlank(stringOf(metadata.get("cellLabel")), readable(cellCode));
                byCell.computeIfAbsent(cellCode, ignored -> new CellAccumulator(cellCode, cellLabel)).accept(item, metadata, now);
            }
        }
        List<CoverageSlice> slices = byCell.values().stream().map(CellAccumulator::toSlice).toList();
        LinkedHashMap<String, Object> metrics = aggregateMetrics(slices);
        LinkedHashSet<String> gaps = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (CoverageSlice slice : slices) {
            if (slice.unassignedItems() > 0) {
                gaps.add("ITENS_SEM_ATRIBUICAO:" + slice.sliceKey());
            }
            if (slice.loadBand().startsWith("CRIT")) {
                warnings.add("Cobertura crítica em " + slice.label() + "; redistribuição e cobertura substitutiva recomendadas.");
            }
            if (slice.blockingItems() > 0 && slice.substitutePool().isEmpty()) {
                warnings.add("Bloqueio operacional sem pool de substitutos materializado em " + slice.label() + '.');
            }
        }
        if (slices.isEmpty()) {
            warnings.add("Nenhum item ativo encontrado para a malha de cobertura da secretaria.");
        }
        return new CoverageProjection(
                inboxKey,
                "SECRETARIAT_COVERAGE",
                List.copyOf(slices),
                Map.copyOf(metrics),
                List.copyOf(gaps),
                List.copyOf(warnings)
        );
    }

    public CoverageProjection resolveInstitutional(String branchCode, Collection<WorkItem> items) {
        Instant now = Instant.now();
        LinkedHashMap<String, CellAccumulator> byOwner = new LinkedHashMap<>();
        if (items != null) {
            for (WorkItem item : items) {
                String ownerCode = item == null || item.getAssignedUser() == null ? "SEM_DONO" : "USR_" + item.getAssignedUser().getId();
                String ownerLabel = item == null || item.getAssignedUser() == null ? "Sem dono" : firstNonBlank(item.getAssignedUser().getNome(), ownerCode);
                byOwner.computeIfAbsent(ownerCode, ignored -> new CellAccumulator(ownerCode, ownerLabel)).accept(item, now);
            }
        }
        List<CoverageSlice> slices = byOwner.values().stream().map(CellAccumulator::toSlice).toList();
        LinkedHashMap<String, Object> metrics = aggregateMetrics(slices);
        LinkedHashSet<String> gaps = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (CoverageSlice slice : slices) {
            if (slice.unassignedItems() > 0) {
                gaps.add("ITEM_INSTITUCIONAL_SEM_RESPONSAVEL:" + slice.sliceKey());
            }
            if (slice.overdueItems() > 0) {
                warnings.add("Há itens institucionais vencidos sob " + slice.label() + '.');
            }
        }
        if (slices.isEmpty()) {
            warnings.add("A cobertura institucional não encontrou work items ativos no órgão consultado.");
        }
        return new CoverageProjection(branchCode, "INSTITUTIONAL_COVERAGE", List.copyOf(slices), Map.copyOf(metrics), List.copyOf(gaps), List.copyOf(warnings));
    }

    private LinkedHashMap<String, Object> aggregateMetrics(List<CoverageSlice> slices) {
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        long totalItems = 0;
        long overdueItems = 0;
        long blockingItems = 0;
        long unassignedItems = 0;
        Instant nextDueAt = null;
        for (CoverageSlice slice : slices) {
            totalItems += slice.totalItems();
            overdueItems += slice.overdueItems();
            blockingItems += slice.blockingItems();
            unassignedItems += slice.unassignedItems();
            nextDueAt = min(nextDueAt, slice.nextDueAt());
        }
        metrics.put("cells", slices.size());
        metrics.put("totalItems", totalItems);
        metrics.put("overdueItems", overdueItems);
        metrics.put("blockingItems", blockingItems);
        metrics.put("unassignedItems", unassignedItems);
        metrics.put("nextDueAt", nextDueAt);
        metrics.put("coverageBand", resolveBand(totalItems, overdueItems, blockingItems, unassignedItems));
        return metrics;
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(raw, MAP_TYPE);
            return map == null ? Map.of() : Map.copyOf(map);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private static String readable(String token) {
        if (token == null || token.isBlank()) {
            return "Célula geral";
        }
        return token.replace('_', ' ');
    }

    private static String stringOf(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Instant min(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return Comparator.<Instant>naturalOrder().compare(first, second) <= 0 ? first : second;
    }

    private static String resolveBand(long totalItems, long overdueItems, long blockingItems, long unassignedItems) {
        if (blockingItems > 0 || overdueItems > 3 || unassignedItems > 1) {
            return "CRITICA";
        }
        if (overdueItems > 0 || unassignedItems > 0 || totalItems > 12) {
            return "ALTA";
        }
        if (totalItems > 5) {
            return "MODERADA";
        }
        return "ESTAVEL";
    }

    public record CoverageProjection(
            String scopeKey,
            String mode,
            List<CoverageSlice> slices,
            Map<String, Object> metrics,
            List<String> gaps,
            List<String> warnings
    ) {
    }

    public record CoverageSlice(
            String sliceKey,
            String label,
            long totalItems,
            long overdueItems,
            long blockingItems,
            long unassignedItems,
            String loadBand,
            Instant nextDueAt,
            List<String> substitutePool,
            List<String> redistributionSuggestions,
            Map<String, Object> metrics
    ) {
    }

    private static final class CellAccumulator {
        private final String cellCode;
        private final String cellLabel;
        private long totalItems;
        private long overdueItems;
        private long blockingItems;
        private long unassignedItems;
        private Instant nextDueAt;
        private final LinkedHashSet<String> substitutePool = new LinkedHashSet<>();
        private final LinkedHashSet<String> redistributionSuggestions = new LinkedHashSet<>();
        private final LinkedHashMap<String, Long> queueLoad = new LinkedHashMap<>();

        private CellAccumulator(String cellCode, String cellLabel) {
            this.cellCode = cellCode;
            this.cellLabel = cellLabel;
        }

        private void accept(SecretariatQueueItem item, Map<String, Object> metadata, Instant now) {
            if (item == null) {
                return;
            }
            totalItems++;
            if (item.getDueAt() != null && item.getDueAt().isBefore(now)) {
                overdueItems++;
            }
            if (item.isBlocking()) {
                blockingItems++;
            }
            boolean assigned = isAssigned(item, metadata);
            if (!assigned) {
                unassignedItems++;
                redistributionSuggestions.add("REDISTRIBUIR_PARA_POOL_ATIVO");
            }
            if (item.isEscalationRequired()) {
                redistributionSuggestions.add("ESCALONAR_PARA_COORDENACAO");
            }
            if (item.isHearingSensitive()) {
                redistributionSuggestions.add("ALOCAR_CELULA_AUDIENCIA");
            }
            nextDueAt = min(nextDueAt, item.getDueAt());
            queueLoad.merge(firstNonBlank(normalize(item.getQueueCode()), "SEM_FILA"), 1L, Long::sum);
            substitutePool.addAll(readPool(metadata, "substitutePool"));
            substitutePool.addAll(readPool(metadata, "coverCandidates"));
        }

        private void accept(WorkItem item, Instant now) {
            if (item == null) {
                return;
            }
            totalItems++;
            if (item.getDueAt() != null && item.getDueAt().isBefore(now)) {
                overdueItems++;
            }
            if (item.isBlocking()) {
                blockingItems++;
            }
            if (item.getAssignedUser() == null) {
                unassignedItems++;
                redistributionSuggestions.add("ATRIBUIR_A_EQUIPE_ATIVA");
            } else {
                substitutePool.add(firstNonBlank(item.getAssignedUser().getNome(), "USR_" + item.getAssignedUser().getId()));
            }
            nextDueAt = min(nextDueAt, item.getDueAt());
            queueLoad.merge(firstNonBlank(normalize(item.getQueueCode()), "SEM_FILA"), 1L, Long::sum);
            if (item.getDueAt() != null && item.getDueAt().isBefore(now)) {
                redistributionSuggestions.add("PRIORIZAR_REVISAO_DE_PRAZO");
            }
        }

        private CoverageSlice toSlice() {
            String loadBand = resolveBand(totalItems, overdueItems, blockingItems, unassignedItems);
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("dominantQueue", queueLoad.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("SEM_FILA"));
            metrics.put("queueSpread", Map.copyOf(queueLoad));
            metrics.put("coverageBand", loadBand);
            return new CoverageSlice(
                    cellCode,
                    cellLabel,
                    totalItems,
                    overdueItems,
                    blockingItems,
                    unassignedItems,
                    loadBand,
                    nextDueAt,
                    List.copyOf(substitutePool),
                    List.copyOf(redistributionSuggestions),
                    Map.copyOf(metrics)
            );
        }

        private boolean isAssigned(SecretariatQueueItem item, Map<String, Object> metadata) {
            if (metadata.containsKey("assignedUserId") || metadata.containsKey("assignedUserName") || metadata.containsKey("responsavel") || metadata.containsKey("ownerUserId")) {
                return true;
            }
            return item != null && (item.isBlocking() || item.isHearingSensitive()) && firstNonBlank(normalize(item.getDeskAxis()), normalize(item.getLaneCode()), normalize(item.getQueueCode())) != null;
        }

        private List<String> readPool(Map<String, Object> metadata, String key) {
            LinkedHashSet<String> out = new LinkedHashSet<>();
            Object raw = metadata.get(key);
            if (raw instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item != null && !String.valueOf(item).isBlank()) {
                        out.add(String.valueOf(item).trim());
                    }
                }
            }
            return List.copyOf(out);
        }
    }
}
