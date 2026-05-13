package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelTelemetriaConectorAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelTelemetriaConectorItem;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorDataPlaneReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorDataPlaneSystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilityReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilitySystemReport;
import com.tcc.pjb.backend.judicial.connectors.application.JudicialConnectorHubService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelTelemetriaConectorApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final JudicialConnectorHubService judicialConnectorHubService;

    public ProcessoPainelTelemetriaConectorApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                              JudicialConnectorHubService judicialConnectorHubService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.judicialConnectorHubService = Objects.requireNonNull(judicialConnectorHubService);
    }

    public ProcessoPainelTelemetriaConectorAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        String tribunalCodigo = unificado.competencia().tribunalCodigo();
        JudicialConnectorHubReport hubReport = tribunalCodigo == null || tribunalCodigo.isBlank()
                ? judicialConnectorHubService.nationalReport(Duration.ofHours(24))
                : judicialConnectorHubService.tribunalReport(tribunalCodigo, Duration.ofHours(24));
        JudicialConnectorCommandCenterReport operational = hubReport.operational();
        JudicialConnectorObservabilityReport observability = operational == null ? null : operational.observability();
        JudicialConnectorDataPlaneReport dataPlane = operational == null ? null : operational.dataPlane();

        Map<String, JudicialConnectorObservabilitySystemReport> observabilityIndex = new LinkedHashMap<>();
        if (observability != null && observability.systems() != null) {
            observability.systems().forEach(item -> observabilityIndex.put(code(item), item));
        }
        Map<String, JudicialConnectorDataPlaneSystemReport> dataPlaneIndex = new LinkedHashMap<>();
        if (dataPlane != null && dataPlane.systems() != null) {
            dataPlane.systems().forEach(item -> dataPlaneIndex.put(code(item), item));
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        codes.addAll(observabilityIndex.keySet());
        codes.addAll(dataPlaneIndex.keySet());

        ArrayList<ProcessoPainelTelemetriaConectorItem> items = new ArrayList<>();
        for (String code : codes) {
            JudicialConnectorObservabilitySystemReport obs = observabilityIndex.get(code);
            JudicialConnectorDataPlaneSystemReport data = dataPlaneIndex.get(code);
            items.add(buildItem(code, obs, data));
        }
        items.sort(Comparator.comparing(ProcessoPainelTelemetriaConectorItem::status).reversed()
                .thenComparing(ProcessoPainelTelemetriaConectorItem::connectorCode));

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (hubReport.alerts() != null) {
            alertas.addAll(hubReport.alerts());
        }
        if (observability != null && observability.alerts() != null) {
            alertas.addAll(observability.alerts());
        }
        if (dataPlane != null && dataPlane.alerts() != null) {
            alertas.addAll(dataPlane.alerts());
        }
        if (items.stream().anyMatch(item -> "ABERTO".equals(item.circuitMode()))) {
            alertas.add("CIRCUIT_BREAKER_ABERTO_EM_PELO_MENOS_UM_CONECTOR");
        }
        if (items.stream().anyMatch(item -> "ULTIMO_ESTADO_CACHE".equals(item.fallbackMode()))) {
            alertas.add("FALLBACK_EXPLICITO_ATIVO_POR_CACHE");
        }
        return new ProcessoPainelTelemetriaConectorAggregate(
                processoId,
                unificado.identity().numeroProcesso(),
                tribunalCodigo,
                tribunalCodigo == null || tribunalCodigo.isBlank() ? "NACIONAL" : "TRIBUNAL",
                List.copyOf(items),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private ProcessoPainelTelemetriaConectorItem buildItem(String code,
                                                           JudicialConnectorObservabilitySystemReport obs,
                                                           JudicialConnectorDataPlaneSystemReport data) {
        String status = obs != null && obs.observabilityStatus() != null ? obs.observabilityStatus() : data != null ? safe(data.executionStatus()) : "UNKNOWN";
        boolean submissionReady = obs != null ? obs.submissionReady() : data != null && data.submissionReady();
        boolean syncReady = obs != null ? obs.syncReady() : data != null && data.syncReady();
        double successRate = obs != null ? obs.successRate() : data != null ? data.successRate() : 0d;
        Instant latestEvent = data != null && data.latestEventAt() != null ? data.latestEventAt() : obs != null ? obs.latestEventAt() : null;
        String fallbackMode = resolveFallbackMode(status, data);
        String cacheMode = data != null && data.snapshotHits() > 0 ? "CACHE_ATIVO" : "CACHE_FRIO";
        String circuitMode = resolveCircuitMode(status, submissionReady, syncReady);
        return new ProcessoPainelTelemetriaConectorItem(
                code,
                status,
                color(status),
                successRate,
                submissionReady,
                syncReady,
                fallbackMode,
                cacheMode,
                circuitMode,
                latencyDescriptor(obs, data),
                latestEvent,
                latestEvent,
                sourceEndpoints(obs, data),
                merge(obs == null ? List.of() : obs.blockers(), data == null ? List.of() : data.blockers()),
                merge(obs == null ? List.of() : obs.warnings(), data == null ? List.of() : data.warnings())
        );
    }

    private String code(JudicialConnectorObservabilitySystemReport item) {
        return item == null || item.system() == null ? "OUTRO" : item.system().name();
    }

    private String code(JudicialConnectorDataPlaneSystemReport item) {
        return item == null || item.system() == null ? "OUTRO" : item.system().name();
    }

    private String resolveFallbackMode(String status, JudicialConnectorDataPlaneSystemReport data) {
        String normalized = safe(status).toUpperCase(Locale.ROOT);
        if (normalized.contains("BLOCK")) {
            return "MANUAL_ASSISTIDO";
        }
        if (data != null && data.snapshotHits() > 0 && !data.syncReady()) {
            return "ULTIMO_ESTADO_CACHE";
        }
        if (data != null && !data.submissionReady() && data.totalEvents() > 0) {
            return "REPLAY_CONTROLADO";
        }
        return "ONLINE";
    }

    private String resolveCircuitMode(String status, boolean submissionReady, boolean syncReady) {
        String normalized = safe(status).toUpperCase(Locale.ROOT);
        if (normalized.contains("BLOCK")) {
            return "ABERTO";
        }
        if (!submissionReady || !syncReady || normalized.contains("DEGRAD")) {
            return "SEMI_ABERTO";
        }
        return "FECHADO";
    }

    private String latencyDescriptor(JudicialConnectorObservabilitySystemReport obs, JudicialConnectorDataPlaneSystemReport data) {
        Object candidate = obs != null && obs.metadata() != null ? obs.metadata().get("latencyMs") : null;
        if (candidate == null && data != null && data.metadata() != null) {
            candidate = data.metadata().get("latencyMs");
        }
        if (candidate instanceof Number number) {
            long value = number.longValue();
            if (value <= 100L) return "SUB_100MS";
            if (value <= 300L) return "SUB_300MS";
            if (value <= 1000L) return "SUB_1S";
            return "ACIMA_1S";
        }
        if (data != null && data.snapshotHits() > 0) {
            return "CACHE_LAST_KNOWN";
        }
        String normalized = safe(obs != null ? obs.observabilityStatus() : data != null ? data.executionStatus() : null).toUpperCase(Locale.ROOT);
        if (normalized.contains("BLOCK")) return "INDISPONIVEL";
        if (normalized.contains("DEGRAD")) return "OSCILANTE";
        return "NAO_MEDIDO";
    }

    private List<String> sourceEndpoints(JudicialConnectorObservabilitySystemReport obs,
                                         JudicialConnectorDataPlaneSystemReport data) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Object baseObs = obs != null && obs.metadata() != null ? obs.metadata().get("baseUrl") : null;
        Object baseData = data != null && data.metadata() != null ? data.metadata().get("baseUrl") : null;
        if (baseObs instanceof String text && !text.isBlank()) {
            out.add(text);
        }
        if (baseData instanceof String text && !text.isBlank()) {
            out.add(text);
        }
        Object messages = data != null && data.metadata() != null ? data.metadata().get("latestMessages") : null;
        if (messages instanceof List<?> list) {
            list.stream().filter(String.class::isInstance).map(String.class::cast).limit(2).forEach(out::add);
        }
        return out.stream().limit(4).toList();
    }

    private List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (first != null) out.addAll(first);
        if (second != null) out.addAll(second);
        return out.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).limit(8).toList();
    }

    private String color(String status) {
        String normalized = safe(status).toUpperCase(Locale.ROOT);
        if (normalized.contains("BLOCK")) return "#DC2626";
        if (normalized.contains("DEGRAD") || normalized.contains("STALE") || normalized.contains("NO_ACTIVITY")) return "#D97706";
        return "#059669";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
