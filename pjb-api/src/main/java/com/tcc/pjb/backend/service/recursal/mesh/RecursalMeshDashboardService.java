package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPrecedentTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardBucket;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardResponse;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class RecursalMeshDashboardService {

    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final RecursalMeshProjectionService projectionService;
    private final ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;
    private final ObjectMapper objectMapper;

    RecursalMeshDashboardService(RecursalProcessIntegrationStateRepository projectionRepository,
                                 RecursalMeshProjectionService projectionService,
                                 ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                 ObjectMapper objectMapper) {
        this(projectionRepository, projectionService, queryRepositoryProvider, null, objectMapper);
    }

    @Inject
    @Autowired
    public RecursalMeshDashboardService(RecursalProcessIntegrationStateRepository projectionRepository,
                                        RecursalMeshProjectionService projectionService,
                                        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider,
                                        ObjectMapper objectMapper) {
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.queryRepositoryProvider = Objects.requireNonNull(queryRepositoryProvider);
        this.telemetryProvider = telemetryProvider;
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public RecursalMeshDashboardResponse dashboard(RecursalMeshDashboardRequest request) {
        RecursalMeshDashboardRequest normalized = normalize(request);
        RecursalMeshQueryRepository queryRepository = queryRepositoryProvider.getIfAvailable();
        if (queryRepository != null && normalized.processoId() == null && normalized.processoIds().isEmpty()) {
            try {
                List<MeshRecord> records = queryRepository.findAll(PageRequest.of(0, normalized.scanLimit(), Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("lastTransitionAt"))))
                        .stream()
                        .map(this::toRecord)
                        .filter(record -> matches(record, normalized))
                        .toList();
                return summarize("SEARCH_INDEX", records, normalized.bucketLimit());
            } catch (RuntimeException ignored) {
            }
        }
        var fallbackPageable = PageRequest.of(0, normalized.scanLimit(), Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("lastTransitionAt")));
        List<MeshRecord> fallback = (!normalized.processoIds().isEmpty() && normalized.processoId() == null
                ? projectionRepository.findByProcesso_IdIn(normalized.processoIds(), fallbackPageable)
                : projectionRepository.findAll(fallbackPageable))
                .stream()
                .map(this::toRecord)
                .filter(record -> matches(record, normalized))
                .toList();
        return summarize("RELATIONAL_FALLBACK", fallback, normalized.bucketLimit());
    }

    private RecursalMeshDashboardRequest normalize(RecursalMeshDashboardRequest request) {
        return request == null
                ? new RecursalMeshDashboardRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 500, 10)
                : request;
    }

    private MeshRecord toRecord(RecursalMeshQueryModel model) {
        return new MeshRecord(
                model.getRecursoId(),
                model.getProcessoId(),
                upper(model.getSpeciesCode()),
                parseEnum(RecursalLifecycleState.class, model.getCurrentState()),
                parseEnum(RecursalTribunal.class, model.getTribunalAtual()),
                parseEnum(RecursalTribunalDetalhado.class, model.getTribunalDetalhadoAtual()),
                parseEnum(RecursalAuthority.class, model.getAutoridadeAtual()),
                trimToNull(model.getPrecedenteCodigo()),
                upper(model.getPrecedenteTribunal()),
                trimToNull(model.getPrecedenteTema()),
                Boolean.TRUE.equals(model.getSobrestadoPrecedente()),
                Boolean.TRUE.equals(model.getPrecedenteAplicado()),
                Boolean.TRUE.equals(model.getPrecedenteDistinguido()),
                Boolean.TRUE.equals(model.getTransitadoEmJulgado()),
                Boolean.TRUE.equals(model.getSlaVencido()),
                Boolean.TRUE.equals(model.getSlaFatalParaPartes()),
                trimToNull(model.getSlaSeveridade())
        );
    }

    private MeshRecord toRecord(RecursalProcessIntegrationState projection) {
        RecursalStateSnapshot snapshot = snapshotOf(projection.getSnapshotJson());
        RecursalPrecedentTrace precedentTrace = snapshot == null ? RecursalPrecedentTrace.empty() : snapshot.precedentTrace();
        RecursalSlaSnapshot sla = projectionService.slaSnapshotOf(projection).orElse(null);
        return new MeshRecord(
                projection.getRecursoId(),
                projection.getProcesso() == null ? null : projection.getProcesso().getId(),
                upper(projection.getSpeciesCode()),
                projection.getCurrentState(),
                projection.getTribunalAtual(),
                projection.getTribunalDetalhadoAtual(),
                projection.getAutoridadeAtual(),
                trimToNull(precedentTrace.precedenteCodigo()),
                upper(precedentTrace.precedenteTribunal()),
                trimToNull(precedentTrace.precedenteTema()),
                snapshot != null && snapshot.sobrestadoPorPrecedente(),
                precedentTrace.aplicado(),
                precedentTrace.distinguido(),
                projection.isTransitadoEmJulgado(),
                sla != null && sla.vencido(),
                sla != null && sla.fatalParaPartes(),
                sla == null ? null : trimToNull(sla.severidade())
        );
    }

    private RecursalStateSnapshot snapshotOf(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RecursalStateSnapshot.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean matches(MeshRecord record, RecursalMeshDashboardRequest request) {
        if (record == null) {
            return false;
        }
        if (request.processoId() != null && !Objects.equals(record.processoId(), request.processoId())) {
            return false;
        }
        if (request.processoId() == null && !request.processoIds().isEmpty() && !request.processoIds().contains(record.processoId())) {
            return false;
        }
        if (hasText(request.speciesCode()) && !equalsIgnoreCase(record.speciesCode(), request.speciesCode())) {
            return false;
        }
        if (request.currentState() != null && record.currentState() != request.currentState()) {
            return false;
        }
        if (request.tribunalAtual() != null && record.tribunalAtual() != request.tribunalAtual()) {
            return false;
        }
        if (request.tribunalDetalhadoAtual() != null && record.tribunalDetalhadoAtual() != request.tribunalDetalhadoAtual()) {
            return false;
        }
        if (request.autoridadeAtual() != null && record.autoridadeAtual() != request.autoridadeAtual()) {
            return false;
        }
        if (hasText(request.precedenteCodigo()) && !equalsIgnoreCase(record.precedenteCodigo(), request.precedenteCodigo())) {
            return false;
        }
        if (hasText(request.precedenteTribunal()) && !equalsIgnoreCase(record.precedenteTribunal(), request.precedenteTribunal())) {
            return false;
        }
        if (hasText(request.precedenteTema()) && !containsIgnoreCase(record.precedenteTema(), request.precedenteTema())) {
            return false;
        }
        if (request.sobrestadoPrecedente() != null && record.sobrestadoPrecedente() != request.sobrestadoPrecedente()) {
            return false;
        }
        if (request.precedenteAplicado() != null && record.precedenteAplicado() != request.precedenteAplicado()) {
            return false;
        }
        if (request.precedenteDistinguido() != null && record.precedenteDistinguido() != request.precedenteDistinguido()) {
            return false;
        }
        if (request.transitadoEmJulgado() != null && record.transitadoEmJulgado() != request.transitadoEmJulgado()) {
            return false;
        }
        if (request.slaVencido() != null && record.slaVencido() != request.slaVencido()) {
            return false;
        }
        if (request.slaFatalParaPartes() != null && record.slaFatalParaPartes() != request.slaFatalParaPartes()) {
            return false;
        }
        if (hasText(request.q())) {
            String haystack = Stream.of(
                            record.speciesCode(),
                            enumName(record.currentState()),
                            enumName(record.tribunalAtual()),
                            enumName(record.tribunalDetalhadoAtual()),
                            enumName(record.autoridadeAtual()),
                            record.precedenteCodigo(),
                            record.precedenteTribunal(),
                            record.precedenteTema(),
                            record.slaSeveridade())
                    .filter(Objects::nonNull)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(" | "));
            if (!haystack.contains(request.q().trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private RecursalMeshDashboardResponse summarize(String source, List<MeshRecord> records, int bucketLimit) {
        long totalItens = records.size();
        long totalSobrestados = records.stream().filter(MeshRecord::sobrestadoPrecedente).count();
        long totalSlaVencido = records.stream().filter(MeshRecord::slaVencido).count();
        long totalSlaFatal = records.stream().filter(MeshRecord::slaFatalParaPartes).count();
        long totalAplicado = records.stream().filter(MeshRecord::precedenteAplicado).count();
        long totalDistinguido = records.stream().filter(MeshRecord::precedenteDistinguido).count();
        List<MeshRecord> gargaloBase = records.stream().filter(MeshRecord::slaVencido).toList();
        if (gargaloBase.isEmpty()) {
            gargaloBase = records;
        }
        RecursalMeshDashboardResponse response = new RecursalMeshDashboardResponse(
                source,
                totalItens,
                totalSobrestados,
                totalSlaVencido,
                totalSlaFatal,
                totalAplicado,
                totalDistinguido,
                bucket(gargaloBase, record -> enumName(record.currentState()), bucketLimit),
                bucket(gargaloBase, record -> enumName(record.tribunalAtual()), bucketLimit),
                bucket(gargaloBase, record -> enumName(record.autoridadeAtual()), bucketLimit),
                bucket(records, record -> enumName(record.tribunalAtual()), bucketLimit),
                bucket(records, record -> enumName(record.autoridadeAtual()), bucketLimit),
                bucket(records, MeshRecord::slaSeveridade, bucketLimit),
                bucket(records, this::temaBucket, bucketLimit)
        );
        if (telemetryProvider != null) {
            RecursalMeshOperationalTelemetryService telemetryService = telemetryProvider.getIfAvailable();
            if (telemetryService != null) {
                telemetryService.updateDashboard(response);
            }
        }
        return response;
    }

    private List<RecursalMeshDashboardBucket> bucket(List<MeshRecord> records,
                                                     Function<MeshRecord, String> classifier,
                                                     int bucketLimit) {
        Map<String, Long> counts = records.stream()
                .map(classifier)
                .map(RecursalMeshDashboardService::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(bucketLimit)
                .map(entry -> new RecursalMeshDashboardBucket(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String temaBucket(MeshRecord record) {
        if (record == null) {
            return null;
        }
        String tema = trimToNull(record.precedenteTema());
        String codigo = trimToNull(record.precedenteCodigo());
        if (tema == null) {
            return codigo;
        }
        if (codigo == null) {
            return tema;
        }
        return codigo + " — " + tema;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right.trim());
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null && haystack.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String upper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record MeshRecord(
            String recursoId,
            Long processoId,
            String speciesCode,
            RecursalLifecycleState currentState,
            RecursalTribunal tribunalAtual,
            RecursalTribunalDetalhado tribunalDetalhadoAtual,
            RecursalAuthority autoridadeAtual,
            String precedenteCodigo,
            String precedenteTribunal,
            String precedenteTema,
            boolean sobrestadoPrecedente,
            boolean precedenteAplicado,
            boolean precedenteDistinguido,
            boolean transitadoEmJulgado,
            boolean slaVencido,
            boolean slaFatalParaPartes,
            String slaSeveridade) {
    }
}
