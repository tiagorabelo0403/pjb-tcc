package com.tcc.pjb.backend.core.transito;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.model.dto.transito.ExecutionPanelActionResponse;
import com.tcc.pjb.backend.model.dto.transito.ExecutionPanelLaneResponse;
import com.tcc.pjb.backend.model.dto.transito.ExecutionPanelResponse;
import com.tcc.pjb.backend.model.dto.transito.ExecutionPanelSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ExecutionPanelAssemblerService {

    private final TransitoJulgadoArquivamentoEngine engine;
    private final Cache<Long, ExecutionPanelResponse> panelCache;

    public ExecutionPanelAssemblerService(TransitoJulgadoArquivamentoEngine engine) {
        this.engine = Objects.requireNonNull(engine);
        this.panelCache = Caffeine.newBuilder()
                .expireAfterWrite(java.time.Duration.ofSeconds(2))
                .maximumSize(512)
                .build();
    }

    public ExecutionPanelResponse assemble(Long processoId) {
        return panelCache.get(Objects.requireNonNull(processoId), this::assembleFresh);
    }

    private ExecutionPanelResponse assembleFresh(Long processoId) {
        Map<String, Object> diagnostic = engine.diagnosticarMalhaExecutiva(processoId);
        Map<String, Object> snapshot = mapValue(diagnostic.get("snapshotExecutivo"));
        ExecutionPanelSummaryResponse summary = new ExecutionPanelSummaryResponse(
                stringValue(snapshot.get("speciesCode")),
                stringValue(snapshot.get("currentStage")),
                stringValue(snapshot.get("currentQueue")),
                stringValue(snapshot.get("currentInbox")),
                stringValue(snapshot.get("currentImpact")),
                stringValue(snapshot.get("currentAssetKind")),
                stringValue(snapshot.get("currentGateway")),
                stringValue(snapshot.get("externalStatus")),
                stringValue(snapshot.get("terminalDisposition")),
                stringValue(snapshot.get("satisfactionState")),
                decimalValue(snapshot.get("satisfactionPercent")),
                decimalValue(snapshot.get("residualAmount")),
                intValue(snapshot.get("incidentCount")),
                intValue(snapshot.get("enforcementCount"))
        );
        List<ExecutionPanelLaneResponse> lanes = List.of(
                lane("INCIDENTES", snapshot, diagnostic, "incidentMatrix", "incidentLedger", "currentStage", "INCIDENTE"),
                lane("ATOS", snapshot, diagnostic, "actMatrix", "enforcementLedger", "currentStage", "PENHORA", "BLOQUEIO", "AVALIACAO", "ALIENACAO", "SATISFACAO", "EXTINCAO"),
                lane("PATRIMONIO", snapshot, diagnostic, "patrimonialMatrix", "patrimonialLedger", "currentAssetKind", "DINHEIRO", "FATURAMENTO", "IMOVEL", "VEICULO", "QUOTAS"),
                lane("INTEGRACAO_EXTERNA", snapshot, diagnostic, "externalConstrictionMatrix", "externalLedger", "currentGateway", "SISBAJUD", "RENAJUD", "CNIB", "OFICIO"),
                lane("EXPROPRIACAO", snapshot, diagnostic, "expropriationMatrix", "expropriationLedger", "currentExpropriationMode", "ADJUDICACAO", "ALIENACAO", "HASTA"),
                lane("LEILAO", snapshot, diagnostic, "auctionCycleMatrix", "auctionCycleLedger", "currentAuctionCycleMode", "PRACA", "LEILAO"),
                lane("CONTINGENCIA", snapshot, diagnostic, "contingencyMatrix", "contingencyLedger", "currentContingencyMode", "CONTING"),
                lane("RECONCILIACAO", snapshot, diagnostic, "reconciliationMatrix", "reconciliationLedger", "reconciliationStatus", "RECONCILI"),
                lane("HOMOLOGACAO", snapshot, diagnostic, "homologationMatrix", "homologationLedger", "currentHomologationMode", "HOMOLOG"),
                lane("LIQUIDACAO", snapshot, diagnostic, "settlementMatrix", "settlementLedger", "currentPreferenceMode", "PREFER", "SUBROG", "LIQUID"),
                lane("FECHAMENTO", snapshot, diagnostic, "closureGovernanceMatrix", "closureLedger", "currentClosureMode", "FECH"),
                lane("TERMINAL", snapshot, diagnostic, "terminalMatrix", "terminalLedger", "terminalDisposition", "SATISFACAO", "EXTINCAO", "SUSPENSAO"),
                lane("ARQUIVO", snapshot, diagnostic, "archiveLinkMatrix", "archiveLedger", "archiveLinkStatus", "ARQUIV", "DESARQUIV")
        );
        List<ExecutionPanelActionResponse> actions = suggestedActions(processoId, diagnostic, snapshot, summary);
        Map<String, Object> integrity = new LinkedHashMap<>();
        integrity.put("aggregateId", snapshot.get("aggregateId"));
        integrity.put("integrityFingerprint", snapshot.get("integrityFingerprint"));
        integrity.put("updatedAt", snapshot.getOrDefault("updatedAt", Instant.now()));
        integrity.put("closureConsistencyStatus", snapshot.get("closureConsistencyStatus"));
        integrity.put("reconciliationStatus", snapshot.get("reconciliationStatus"));
        integrity.values().removeIf(Objects::isNull);
        Map<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("refreshHintSeconds", 15);
        frontend.put("defaultTab", resolveDefaultTab(snapshot, lanes));
        frontend.put("primaryTabs", lanes.stream().map(ExecutionPanelLaneResponse::code).toList());
        frontend.put("highlightStage", summary.currentStage());
        frontend.put("stickySummaryFields", List.of("currentStage", "currentQueue", "currentInbox", "terminalDisposition", "residualAmount"));
        frontend.put("ledgerSizes", buildLedgerSizes(snapshot));
        frontend.put("actionCount", actions.size());
        return new ExecutionPanelResponse(
                longValue(diagnostic.get("processoId")),
                stringValue(diagnostic.get("numero")),
                stringValue(diagnostic.get("statusAtual")),
                stringValue(diagnostic.get("faseAtual")),
                booleanValue(diagnostic.get("executionReady")),
                longValue(diagnostic.get("pendenciasOperacionais")),
                longValue(diagnostic.get("bloqueiosOperacionais")),
                summary,
                lanes,
                actions,
                integrity,
                frontend
        );
    }

    private ExecutionPanelLaneResponse lane(String code,
                                            Map<String, Object> snapshot,
                                            Map<String, Object> diagnostic,
                                            String matrixKey,
                                            String ledgerKey,
                                            String referenceKey,
                                            String... hotTokens) {
        Map<String, Object> matrix = mapValue(diagnostic.get(matrixKey));
        List<Map<String, Object>> ledger = listOfMaps(snapshot.get(ledgerKey));
        String reference = stringValue(snapshot.get(referenceKey));
        List<String> highlights = extractHighlights(matrix);
        String status = resolveLaneStatus(reference, ledger.size(), highlights, hotTokens);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reference", reference);
        metadata.put("matrixKeys", List.copyOf(matrix.keySet()));
        metadata.put("ledgerCount", ledger.size());
        metadata.put("hasMatrix", !matrix.isEmpty());
        return new ExecutionPanelLaneResponse(
                code,
                status,
                ledger.size(),
                resolveDescriptor(reference, highlights),
                highlights,
                metadata
        );
    }

    private List<ExecutionPanelActionResponse> suggestedActions(Long processoId,
                                                                Map<String, Object> diagnostic,
                                                                Map<String, Object> snapshot,
                                                                ExecutionPanelSummaryResponse summary) {
        List<ExecutionPanelActionResponse> actions = new ArrayList<>();
        boolean executionReady = booleanValue(diagnostic.get("executionReady"));
        if (!executionReady) {
            actions.add(action("VALIDAR_ELEGIBILIDADE_EXECUTIVA", "Validar elegibilidade executiva", "critical", true,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/malha-executiva",
                    payloadOf("focus", "executionReady")));
        }
        if (blank(summary.currentAssetKind())) {
            actions.add(action("MAPEAR_PATRIMONIO", "Mapear patrimônio constritível", "high", executionReady,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/constricao-patrimonial",
                    payloadOf("suggestedAssetKinds", List.of("DINHEIRO", "IMOVEL", "VEICULO", "QUOTAS_SOCIAIS"))));
        }
        if (!blank(summary.currentGateway()) && blank(summary.externalStatus())) {
            actions.add(action("RECONCILIAR_GATEWAY", "Reconciliar retorno do gateway", "high", true,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/reconciliacao-constricao",
                    payloadOf("gateway", summary.currentGateway())));
        }
        if (!blank(stringValue(snapshot.get("currentExpropriationMode"))) && blank(stringValue(snapshot.get("currentHomologationMode")))) {
            actions.add(action("HOMOLOGAR_EXPROPRIACAO", "Homologar expropriação", "high", true,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/homologacao-expropriacao",
                    payloadOf("assetKind", summary.currentAssetKind())));
        }
        if (!blank(stringValue(snapshot.get("currentHomologationMode"))) && blank(stringValue(snapshot.get("currentClosureMode")))) {
            actions.add(action("LIQUIDAR_E_FECHAR", "Liquidar produto e consolidar fechamento", "medium", true,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/consolidacao-fechamento-executivo",
                    payloadOf("terminalDisposition", stringValue(snapshot.get("terminalDisposition")))));
        }
        if (!blank(stringValue(snapshot.get("terminalDisposition"))) && blank(stringValue(snapshot.get("archiveLinkStatus")))) {
            actions.add(action("VINCULAR_ARQUIVAMENTO", "Vincular terminal ao arquivamento", "medium", true,
                    "/api/v1/processo/transito-julgado/processos/" + processoId + "/vinculo-arquivamento-terminal",
                    payloadOf("closureMode", stringValue(snapshot.get("currentClosureMode")))));
        }
        return List.copyOf(actions);
    }

    private Map<String, Object> payloadOf(String key, Object value) {
        if (value == null) {
            return Map.of();
        }
        return Map.of(key, value);
    }

    private ExecutionPanelActionResponse action(String action,
                                                String label,
                                                String severity,
                                                boolean enabled,
                                                String endpoint,
                                                Map<String, Object> payload) {
        return new ExecutionPanelActionResponse(action, label, severity, enabled, endpoint, payload);
    }

    private Map<String, Object> buildLedgerSizes(Map<String, Object> snapshot) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("incidentLedger", listOfMaps(snapshot.get("incidentLedger")).size());
        out.put("enforcementLedger", listOfMaps(snapshot.get("enforcementLedger")).size());
        out.put("patrimonialLedger", listOfMaps(snapshot.get("patrimonialLedger")).size());
        out.put("externalLedger", listOfMaps(snapshot.get("externalLedger")).size());
        out.put("expropriationLedger", listOfMaps(snapshot.get("expropriationLedger")).size());
        out.put("auctionCycleLedger", listOfMaps(snapshot.get("auctionCycleLedger")).size());
        out.put("contingencyLedger", listOfMaps(snapshot.get("contingencyLedger")).size());
        out.put("reconciliationLedger", listOfMaps(snapshot.get("reconciliationLedger")).size());
        out.put("homologationLedger", listOfMaps(snapshot.get("homologationLedger")).size());
        out.put("settlementLedger", listOfMaps(snapshot.get("settlementLedger")).size());
        out.put("closureLedger", listOfMaps(snapshot.get("closureLedger")).size());
        out.put("terminalLedger", listOfMaps(snapshot.get("terminalLedger")).size());
        out.put("archiveLedger", listOfMaps(snapshot.get("archiveLedger")).size());
        return Collections.unmodifiableMap(out);
    }

    private String resolveDefaultTab(Map<String, Object> snapshot, List<ExecutionPanelLaneResponse> lanes) {
        if (!blank(stringValue(snapshot.get("currentGateway")))) {
            return "INTEGRACAO_EXTERNA";
        }
        if (!blank(stringValue(snapshot.get("currentExpropriationMode")))) {
            return "EXPROPRIACAO";
        }
        return lanes.stream()
                .filter(lane -> !"IDLE".equals(lane.status()) && !"EMPTY".equals(lane.status()))
                .map(ExecutionPanelLaneResponse::code)
                .findFirst()
                .orElse("INCIDENTES");
    }

    private String resolveLaneStatus(String reference, int ledgerCount, List<String> highlights, String... hotTokens) {
        if (ledgerCount > 0) {
            return "ACTIVE";
        }
        if (!blank(reference) && containsAny(reference, hotTokens)) {
            return "HOT";
        }
        if (!highlights.isEmpty()) {
            return "READY";
        }
        return "IDLE";
    }

    private String resolveDescriptor(String reference, List<String> highlights) {
        if (!blank(reference)) {
            return reference;
        }
        return highlights.isEmpty() ? "Sem destaque operacional imediato" : highlights.getFirst();
    }

    private List<String> extractHighlights(Map<String, Object> matrix) {
        if (matrix.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        collectHighlights(matrix, out);
        return out.stream().limit(4).toList();
    }

    private void collectHighlights(Object value, LinkedHashSet<String> out) {
        if (value == null || out.size() >= 4) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                collectHighlights(nested, out);
                if (out.size() >= 4) {
                    return;
                }
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                collectHighlights(nested, out);
                if (out.size() >= 4) {
                    return;
                }
            }
            return;
        }
        String text = stringValue(value);
        if (!blank(text) && text.length() <= 140) {
            out.add(text);
        }
    }

    private boolean containsAny(String value, String... tokens) {
        if (blank(value) || tokens == null || tokens.length == 0) {
            return false;
        }
        String normalized = value.toUpperCase();
        for (String token : tokens) {
            if (!blank(token) && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        converted.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                out.add(Map.copyOf(converted));
            }
        }
        return List.copyOf(out);
    }

    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = stringValue(value);
        if (text == null) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = stringValue(value);
        if (text == null) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = stringValue(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "sim".equalsIgnoreCase(text);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
