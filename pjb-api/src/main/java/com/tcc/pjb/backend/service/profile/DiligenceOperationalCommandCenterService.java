package com.tcc.pjb.backend.service.profile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalCommandCenterResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class DiligenceOperationalCommandCenterService {

    private final CurrentUserService currentUserService;
    private final DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository;
    private final DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository;
    private final ProcessoRepository processoRepository;

    public DiligenceOperationalCommandCenterService(CurrentUserService currentUserService,
                                                    DiligenciaOperadorAnexacaoInstitucionalRepository annexationRepository,
                                                    DiligenciaOperadorMalhaInstitucionalDispatchRepository dispatchRepository,
                                                    ProcessoRepository processoRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.annexationRepository = Objects.requireNonNull(annexationRepository);
        this.dispatchRepository = Objects.requireNonNull(dispatchRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional(readOnly = true)
    public DiligenceOperationalCommandCenterResponse snapshot(TelemetriaOperacionalCanal canal,
                                                              int lookbackDays,
                                                              int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        Usuario actor = currentUserService.getRequired();
        int safeLookback = Math.max(1, Math.min(lookbackDays, 90));
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Instant cutoff = Instant.now().minusSeconds(safeLookback * 86_400L);

        List<DiligenciaOperadorAnexacaoInstitucional> annexations = annexationRepository
                .findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(actor.getId(), canal, cutoff);
        List<DiligenciaOperadorMalhaInstitucionalDispatch> dispatches = dispatchRepository
                .findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(actor.getId(), canal, cutoff);

        Map<Long, Processo> processos = new LinkedHashMap<>();
        LinkedHashSet<Long> processIds = new LinkedHashSet<>();
        annexations.stream().map(DiligenciaOperadorAnexacaoInstitucional::getProcessoId).filter(Objects::nonNull).forEach(processIds::add);
        dispatches.stream().map(DiligenciaOperadorMalhaInstitucionalDispatch::getProcessoId).filter(Objects::nonNull).forEach(processIds::add);
        processoRepository.findAllById(processIds).forEach(item -> processos.put(item.getId(), item));

        Map<Long, ProcessAccumulator> processBuckets = new LinkedHashMap<>();
        Map<String, UnitAccumulator> unitBuckets = new LinkedHashMap<>();
        Map<String, OrganizationAccumulator> organizationBuckets = new LinkedHashMap<>();

        annexations.forEach(item -> {
            Processo processo = processos.get(item.getProcessoId());
            String unitLabel = resolveUnitLabel(actor, processo, item.getDestinationBox());
            String organizationLabel = resolveOrganizationLabel(actor, processo, item.getExternalSystemCode());
            ProcessAccumulator process = processBuckets.computeIfAbsent(item.getProcessoId(), id -> new ProcessAccumulator(
                    item.getProcessoId(),
                    firstNonBlank(item.getProcessoNumero(), processo != null ? processo.getNumeroProcesso() : null),
                    unitLabel,
                    organizationLabel
            ));
            process.annexations++;
            process.stages.add("ANEXACAO_INSTITUCIONAL");
            process.lastAt = max(process.lastAt, item.getCreatedAt());

            UnitAccumulator unit = unitBuckets.computeIfAbsent(unitLabel, UnitAccumulator::new);
            unit.processIds.add(item.getProcessoId());
            unit.lastAt = max(unit.lastAt, item.getCreatedAt());

            OrganizationAccumulator organization = organizationBuckets.computeIfAbsent(organizationLabel, OrganizationAccumulator::new);
            organization.processIds.add(item.getProcessoId());
            organization.unitLabels.add(unitLabel);
            organization.lastAt = max(organization.lastAt, item.getCreatedAt());
        });

        dispatches.forEach(item -> {
            Processo processo = processos.get(item.getProcessoId());
            String unitLabel = resolveUnitLabel(actor, processo, item.getMeshUnitKey());
            String organizationLabel = resolveOrganizationLabel(actor, processo, item.getMeshOrgKey());
            ProcessAccumulator process = processBuckets.computeIfAbsent(item.getProcessoId(), id -> new ProcessAccumulator(
                    item.getProcessoId(),
                    firstNonBlank(item.getProcessoNumero(), processo != null ? processo.getNumeroProcesso() : null),
                    unitLabel,
                    organizationLabel
            ));
            process.dispatches++;
            if ("ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                process.acknowledged++;
            }
            if (!"ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                process.backlog++;
            }
            process.stages.add(item.getDispatchStatus());
            process.lastAt = max(process.lastAt, firstNonNull(item.getAcknowledgedAt(), item.getDeliveredAt(), item.getCreatedAt()));

            UnitAccumulator unit = unitBuckets.computeIfAbsent(unitLabel, UnitAccumulator::new);
            unit.processIds.add(item.getProcessoId());
            unit.dispatches++;
            if ("ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                unit.acknowledged++;
            }
            if (!"ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                unit.backlog++;
            }
            unit.lastAt = max(unit.lastAt, firstNonNull(item.getAcknowledgedAt(), item.getDeliveredAt(), item.getCreatedAt()));

            OrganizationAccumulator organization = organizationBuckets.computeIfAbsent(organizationLabel, OrganizationAccumulator::new);
            organization.processIds.add(item.getProcessoId());
            organization.unitLabels.add(unitLabel);
            organization.dispatches++;
            if ("ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                organization.acknowledged++;
            }
            if (!"ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())) {
                organization.backlog++;
            }
            organization.lastAt = max(organization.lastAt, firstNonNull(item.getAcknowledgedAt(), item.getDeliveredAt(), item.getCreatedAt()));
        });

        List<DiligenceOperationalCommandCenterResponse.ProcessBucket> processResponses = processBuckets.values().stream()
                .sorted(Comparator.comparing(ProcessAccumulator::backlog).reversed()
                        .thenComparing(ProcessAccumulator::lastAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .map(item -> new DiligenceOperationalCommandCenterResponse.ProcessBucket(
                        item.processoId,
                        item.processoNumero,
                        item.unitLabel,
                        item.organizationLabel,
                        item.annexations,
                        item.dispatches,
                        item.acknowledged,
                        item.backlog,
                        item.lastAt,
                        item.stageList()
                ))
                .toList();

        List<DiligenceOperationalCommandCenterResponse.UnitBucket> unitResponses = unitBuckets.values().stream()
                .sorted(Comparator.comparing(UnitAccumulator::backlog).reversed()
                        .thenComparing(UnitAccumulator::lastAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .map(item -> new DiligenceOperationalCommandCenterResponse.UnitBucket(
                        item.unitLabel,
                        item.processIds.size(),
                        item.dispatches,
                        item.acknowledged,
                        item.backlog,
                        item.lastAt
                ))
                .toList();

        List<DiligenceOperationalCommandCenterResponse.OrganizationBucket> organizationResponses = organizationBuckets.values().stream()
                .sorted(Comparator.comparing(OrganizationAccumulator::backlog).reversed()
                        .thenComparing(OrganizationAccumulator::lastAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .map(item -> new DiligenceOperationalCommandCenterResponse.OrganizationBucket(
                        item.organizationLabel,
                        item.processIds.size(),
                        item.unitLabels.size(),
                        item.dispatches,
                        item.acknowledged,
                        item.backlog,
                        item.lastAt
                ))
                .toList();

        long backlog = dispatches.stream().filter(item -> !"ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())).count();
        long acknowledged = dispatches.stream().filter(item -> "ACKNOWLEDGED".equalsIgnoreCase(item.getDispatchStatus())).count();
        long dispatched = dispatches.stream().filter(item -> "DISPATCHED".equalsIgnoreCase(item.getDispatchStatus()) || "DELIVERED_CONFIRMED".equalsIgnoreCase(item.getDispatchStatus())).count();
        long queued = dispatches.stream().filter(item -> "OUTBOX_ENQUEUED".equalsIgnoreCase(item.getDispatchStatus())).count();
        Instant lastMovementAt = max(
                annexations.stream().map(DiligenciaOperadorAnexacaoInstitucional::getCreatedAt).filter(Objects::nonNull).max(Instant::compareTo).orElse(null),
                dispatches.stream().map(item -> firstNonNull(item.getAcknowledgedAt(), item.getDeliveredAt(), item.getCreatedAt())).filter(Objects::nonNull).max(Instant::compareTo).orElse(null)
        );

        List<String> alerts = new ArrayList<>();
        if (backlog > 0) {
            alerts.add("BACKLOG_MALHA=" + backlog);
        }
        if (queued > 0) {
            alerts.add("OUTBOX_PENDENTE=" + queued);
        }
        if (organizationResponses.stream().anyMatch(item -> item.backlog() > 3)) {
            alerts.add("ORGAO_COM_ACUMULO_CRITICO");
        }
        if (lastMovementAt == null) {
            alerts.add("SEM_MOVIMENTACAO_RECENTE");
        }

        return new DiligenceOperationalCommandCenterResponse(
                canal.name(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                resolveBaseUnit(actor),
                new DiligenceOperationalCommandCenterResponse.Summary(
                        annexations.size(),
                        queued,
                        dispatched,
                        acknowledged,
                        backlog,
                        processBuckets.size(),
                        unitBuckets.size(),
                        organizationBuckets.size(),
                        lastMovementAt
                ),
                processResponses,
                unitResponses,
                organizationResponses,
                List.copyOf(alerts)
        );
    }

    private String resolveUnitLabel(Usuario actor, Processo processo, String fallback) {
        return firstNonBlank(
                processo != null ? processo.getUnidadeJudiciariaCodigo() : null,
                actor.getComarca() != null && actor.getUf() != null ? actor.getComarca().trim().toUpperCase() + "/" + actor.getUf().trim().toUpperCase() : null,
                fallback,
                "CENTRAL"
        );
    }

    private String resolveOrganizationLabel(Usuario actor, Processo processo, String fallback) {
        return firstNonBlank(
                processo != null ? processo.getTribunalCodigoRoteado() : null,
                fallback,
                actor.getUf() != null ? "TRIBUNAL_" + actor.getUf().trim().toUpperCase() : null,
                "PJB"
        );
    }

    private String resolveBaseUnit(Usuario actor) {
        return firstNonBlank(
                actor.getComarca() != null && actor.getUf() != null ? actor.getComarca().trim().toUpperCase() + "/" + actor.getUf().trim().toUpperCase() : null,
                actor.getUf(),
                "CENTRAL"
        );
    }

    private static String firstNonBlank(String... values) {
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

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Instant max(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private static final class ProcessAccumulator {
        private final Long processoId;
        private final String processoNumero;
        private final String unitLabel;
        private final String organizationLabel;
        private long annexations;
        private long dispatches;
        private long acknowledged;
        private long backlog;
        private Instant lastAt;
        private final LinkedHashSet<String> stages = new LinkedHashSet<>();

        private ProcessAccumulator(Long processoId, String processoNumero, String unitLabel, String organizationLabel) {
            this.processoId = processoId;
            this.processoNumero = processoNumero;
            this.unitLabel = unitLabel;
            this.organizationLabel = organizationLabel;
        }

        private long backlog() {
            return backlog;
        }

        private Instant lastAt() {
            return lastAt;
        }

        private List<String> stageList() {
            return List.copyOf(stages);
        }
    }

    private static final class UnitAccumulator {
        private final String unitLabel;
        private final LinkedHashSet<Long> processIds = new LinkedHashSet<>();
        private long dispatches;
        private long acknowledged;
        private long backlog;
        private Instant lastAt;

        private UnitAccumulator(String unitLabel) {
            this.unitLabel = unitLabel;
        }

        private long backlog() {
            return backlog;
        }

        private Instant lastAt() {
            return lastAt;
        }
    }

    private static final class OrganizationAccumulator {
        private final String organizationLabel;
        private final LinkedHashSet<Long> processIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> unitLabels = new LinkedHashSet<>();
        private long dispatches;
        private long acknowledged;
        private long backlog;
        private Instant lastAt;

        private OrganizationAccumulator(String organizationLabel) {
            this.organizationLabel = organizationLabel;
        }

        private long backlog() {
            return backlog;
        }

        private Instant lastAt() {
            return lastAt;
        }
    }
}
