package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;

@Service
public class KernelOperationalGovernanceService {

    public KernelOperationalGovernanceReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                            String ritoName,
                                                            LegalCoherenceReport coherence,
                                                            ProtocolDryRunReport dryRun,
                                                            ProcessIntegrityRadarReport radar,
                                                            InstitutionalGovernanceContextReport governance) {
        Objects.requireNonNull(request, "request");
        Set<String> risks = new LinkedHashSet<>();
        Set<String> controls = new LinkedHashSet<>();
        Set<String> nextActions = new LinkedHashSet<>();
        Set<String> watchpoints = new LinkedHashSet<>();
        List<String> governancePolicyGuards = governance != null ? governance.policyGuards() : List.of();
        List<String> governanceAlerts = governance != null ? governance.governanceAlerts() : List.of();
        List<String> governanceEscalation = governance != null ? governance.escalationPlaybooks() : List.of();
        List<String> dryRunNextActions = dryRun != null ? dryRun.nextActions() : List.of();
        double confidence = 0.66d;

        addWhen(controls, !blank(ritoName), "Roteamento operacional amarrado ao rito efetivo antes do protocolo assistido.");
        addWhen(controls, !governancePolicyGuards.isEmpty(), governancePolicyGuards.getFirst());
        addWhen(nextActions, !dryRunNextActions.isEmpty(), dryRunNextActions.getFirst());

        if (coherence != null && coherence.blocking()) {
            risks.add("Coerência jurídica bloqueante impede promover o fluxo como operacionalmente seguro.");
            confidence -= 0.14d;
        }
        if (dryRun != null && !dryRun.apto()) {
            risks.add("Dry run de protocolo sinalizou travas operacionais ou dependências impeditivas.");
            nextActions.addAll(limit(dryRun.nextActions(), 4));
            confidence -= 0.10d;
        }
        if (radar != null && radar.blocking()) {
            risks.addAll(limit(radar.nextActions(), 4));
            watchpoints.addAll(limit(radar.watchpoints(), 4));
            confidence -= 0.12d;
        } else if (radar != null) {
            watchpoints.addAll(limit(radar.watchpoints(), 4));
            confidence += 0.03d;
        }
        if (governance != null) {
            controls.addAll(limit(governancePolicyGuards, 4));
            watchpoints.addAll(limit(governanceAlerts, 4));
            nextActions.addAll(limit(governanceEscalation, 3));
            confidence += governanceAlerts.isEmpty() ? 0.03d : -0.03d;
        }
        if (Boolean.TRUE.equals(request.getCasoUrgente())) {
            watchpoints.add("Urgência declarada exige janela operacional curta e revisão humana reforçada.");
            confidence -= 0.02d;
        }

        String status = risks.isEmpty() ? "REQUEST_KERNEL_STABLE" : "REQUEST_KERNEL_ATTENTION";
        return new KernelOperationalGovernanceReport(
                "PETITION_ASSIST",
                status,
                round(clamp(confidence)),
                List.copyOf(risks),
                List.copyOf(controls),
                List.copyOf(nextActions),
                List.copyOf(watchpoints),
                PayloadMaps.ofEntries(
                        "scope", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "requestProcessoId", request.getProcessoId(),
                        "dryRunStatus", dryRun != null ? dryRun.status() : null,
                        "radarStatus", radar != null ? radar.status() : null
                )
        );
    }

    public KernelOperationalGovernanceReport analyzeProcess(Processo processo,
                                                            String ritoName,
                                                            ProcessIntegrityRadarReport radar,
                                                            ExplainableDecisionTrailReport explainableTrail,
                                                            InstitutionalGovernanceContextReport governance,
                                                            NegotiationMemoryReport negotiationMemory,
                                                            NegotiationExplainabilityReport negotiationExplainability,
                                                            StrategicCopilotReport strategicCopilot,
                                                            InstitutionalMemoryReport institutionalMemory) {
        Objects.requireNonNull(processo, "processo");
        Set<String> risks = new LinkedHashSet<>();
        Set<String> controls = new LinkedHashSet<>();
        Set<String> nextActions = new LinkedHashSet<>();
        Set<String> watchpoints = new LinkedHashSet<>();
        List<String> governancePolicyGuards = governance != null ? governance.policyGuards() : List.of();
        List<String> governanceAlerts = governance != null ? governance.governanceAlerts() : List.of();
        List<String> governanceEscalation = governance != null ? governance.escalationPlaybooks() : List.of();
        List<StrategicCopilotReport.Action> strategicProceduralActions =
                strategicCopilot != null ? strategicCopilot.proceduralActions() : List.of();
        double confidence = 0.69d;

        addWhen(controls, !blank(ritoName), "Kernel operacional sincronizado ao rito efetivo do processo.");
        addWhen(controls, !governancePolicyGuards.isEmpty(), governancePolicyGuards.getFirst());
        addWhen(nextActions, !strategicProceduralActions.isEmpty(), strategicProceduralActions.getFirst().title());

        if (radar != null && radar.blocking()) {
            risks.addAll(limit(radar.nextActions(), 4));
            watchpoints.addAll(limit(radar.watchpoints(), 4));
            confidence -= 0.12d;
        } else if (radar != null) {
            watchpoints.addAll(limit(radar.watchpoints(), 3));
            confidence += 0.02d;
        }
        if (explainableTrail != null && !explainableTrail.openQuestions().isEmpty()) {
            risks.addAll(limit(explainableTrail.openQuestions(), 3));
            confidence -= 0.08d;
        }
        if (governance != null) {
            controls.addAll(limit(governancePolicyGuards, 4));
            nextActions.addAll(limit(governanceEscalation, 3));
            watchpoints.addAll(limit(governanceAlerts, 3));
            confidence += governanceAlerts.isEmpty() ? 0.03d : -0.03d;
        }
        if (negotiationMemory != null) {
            nextActions.addAll(limit(negotiationMemory.reusablePlaybooks(), 3));
            watchpoints.addAll(limit(negotiationMemory.cautionPoints(), 3));
            risks.addAll(limit(negotiationMemory.repeatedFailureModes(), 2));
            confidence += negotiationMemory.repeatedFailureModes().isEmpty() ? 0.02d : -0.05d;
        }
        if (negotiationExplainability != null && !negotiationExplainability.openQuestions().isEmpty()) {
            nextActions.addAll(limit(negotiationExplainability.openQuestions(), 3));
            confidence -= 0.04d;
        }
        if (strategicCopilot != null) {
            strategicCopilot.watchpoints().stream().filter(Objects::nonNull).limit(3).forEach(watchpoints::add);
            strategicProceduralActions.stream().map(StrategicCopilotReport.Action::title).limit(3).forEach(nextActions::add);
            confidence += 0.02d;
        }
        if (institutionalMemory != null) {
            controls.addAll(limit(institutionalMemory.reusablePlaybooks(), 3));
            watchpoints.addAll(limit(institutionalMemory.officeAlerts(), 3));
            confidence += institutionalMemory.repeatedFailureModes().isEmpty() ? 0.02d : -0.03d;
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            controls.add("Fluxo operacional deve respeitar segmentação reforçada por sigilo e credencial.");
            confidence -= 0.03d;
        }

        String status = risks.isEmpty() ? "PROCESS_KERNEL_STABLE" : "PROCESS_KERNEL_ATTENTION";
        return new KernelOperationalGovernanceReport(
                "PROCESS_TWIN",
                status,
                round(clamp(confidence)),
                List.copyOf(risks),
                List.copyOf(controls),
                List.copyOf(nextActions),
                List.copyOf(watchpoints),
                PayloadMaps.ofEntries(
                        "scope", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "phase", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null
                )
        );
    }

    public KernelAdvisoryTelemetry buildTelemetry(String scope, String ritoName, Object... advisoryComponents) {
        List<String> components = new ArrayList<>();
        int blockingCount = 0;
        if (advisoryComponents != null) {
            for (Object component : advisoryComponents) {
                if (component == null) {
                    continue;
                }
                components.add(componentName(component));
                if (isBlocking(component)) {
                    blockingCount++;
                }
            }
        }
        String statusBand = blockingCount == 0 ? "STABLE" : blockingCount <= 2 ? "ATTENTION" : "BLOCKING";
        return new KernelAdvisoryTelemetry(
                scope,
                UUID.randomUUID().toString(),
                statusBand,
                Instant.now(),
                ritoName,
                components.size(),
                blockingCount,
                List.copyOf(components),
                PayloadMaps.ofEntries(
                        "scope", scope,
                        "ritoName", ritoName,
                        "blockingCount", blockingCount,
                        "advisoryCount", components.size()
                )
        );
    }

    private static String componentName(Object component) {
        String simple = component.getClass().getSimpleName();
        return simple.isBlank() ? component.getClass().getName() : simple;
    }

    private static boolean isBlocking(Object component) {
        return switch (component) {
            case LegalCoherenceReport coherence -> coherence.blocking();
            case ProtocolDryRunReport dryRun -> !dryRun.apto();
            case ProcessIntegrityRadarReport radar -> radar.blocking();
            case SettlementAdvisoryReport settlement -> !settlement.executable();
            case InstitutionalMemoryReport memory -> hasAttention(memory.status());
            case ContextualPrecedentAdvisoryReport precedents -> hasAttention(precedents.status());
            case ExplainableDecisionTrailReport trail -> !trail.openQuestions().isEmpty();
            case InstitutionalGovernanceContextReport governance -> hasAttention(governance.status());
            case NegotiationMemoryReport negotiationMemory -> hasAttention(negotiationMemory.status());
            case NegotiationExplainabilityReport explainability -> !explainability.openQuestions().isEmpty();
            case KernelOperationalGovernanceReport kernel -> hasAttention(kernel.status());
            default -> false;
        };
    }

    private static boolean hasAttention(String status) {
        if (blank(status)) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return normalized.contains("ATTENTION") || normalized.contains("REVIEW") || normalized.contains("BLOCK") || normalized.contains("PENDING");
    }

    private static void addWhen(Set<String> target, boolean condition, String value) {
        if (condition && !blank(value)) {
            target.add(value);
        }
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty() || max <= 0) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
