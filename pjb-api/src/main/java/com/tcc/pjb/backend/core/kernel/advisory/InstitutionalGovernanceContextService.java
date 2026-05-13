package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;

@Service
public class InstitutionalGovernanceContextService {

    public InstitutionalGovernanceContextReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                               CanonicalContext canonical,
                                                               String ritoName,
                                                               LegalCoherenceReport coherence,
                                                               InstitutionalMemoryReport memory,
                                                               ContextualPrecedentAdvisoryReport precedents) {
        Objects.requireNonNull(request, "request");
        Set<String> anchors = new LinkedHashSet<>();
        Set<String> alerts = new LinkedHashSet<>();
        Set<String> guards = new LinkedHashSet<>();
        Set<String> escalation = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        String canonicalRamoDireito = canonical != null ? canonical.ramoDireito() : null;
        String canonicalTribunalCodigo = canonical != null ? canonical.tribunalCodigo() : null;
        String canonicalClasseTpu = canonical != null ? canonical.classeTpuCodigo() : null;
        double confidence = 0.67d;

        addWhen(anchors, !blank(ritoName), "rito:" + ritoName);
        addWhen(anchors, !blank(canonicalRamoDireito), "ramo:" + canonicalRamoDireito);
        addWhen(anchors, !blank(canonicalTribunalCodigo), "tribunal:" + canonicalTribunalCodigo);
        addWhen(anchors, !blank(canonicalClasseTpu), "classe:" + canonicalClasseTpu);

        addWhen(keys, !blank(ritoName), "governance:rito:" + ritoName);
        addWhen(keys, !blank(canonicalTribunalCodigo), "governance:tribunal:" + canonicalTribunalCodigo);
        addWhen(keys, !blank(canonicalRamoDireito), "governance:ramo:" + canonicalRamoDireito);

        guards.add("Promover protocolo assistido apenas com aderência entre rito, competência e narrativa nuclear.");
        guards.add("Exigir revisão humana final quando a trilha institucional registrar cautela material ou bloqueio lógico.");

        if (coherence != null && coherence.blocking()) {
            alerts.add("Coerência jurídica bloqueante reduz governança institucional do pedido.");
            escalation.add("Escalar revisão para núcleo sênior antes de qualquer protocolo ou automação adicional.");
            confidence -= 0.16d;
        } else if (coherence != null) {
            guards.addAll(limit(coherence.strengths(), 2));
            confidence += 0.04d;
        }

        if (memory != null) {
            guards.addAll(limit(memory.reusablePlaybooks(), 3));
            alerts.addAll(limit(memory.officeAlerts(), 3));
            escalation.addAll(limit(memory.repeatedFailureModes(), 2));
            confidence += memory.repeatedFailureModes().isEmpty() ? 0.04d : -0.05d;
        }

        if (precedents != null) {
            guards.addAll(limit(precedents.targetDecisionProfiles(), 2));
            alerts.addAll(limit(precedents.cautionPoints(), 3));
            anchors.addAll(limit(precedents.anchorDimensions(), 3));
            confidence += precedents.cautionPoints().isEmpty() ? 0.03d : -0.03d;
        }

        if (truthy(request.getCasoUrgente())) {
            alerts.add("Caso urgente demanda governança reforçada sobre prova, reversibilidade e trilha decisória.");
            escalation.add("Aplicar dupla revisão institucional no bloco de tutela de urgência.");
            confidence -= 0.03d;
        }

        if (blank(request.getCpfCnpjAutor()) || blank(request.getCpfCnpjReu())) {
            alerts.add("Qualificação parcial das partes compromete governança documental do protocolo.");
            guards.add("Travar reaproveitamento institucional até fechar qualificação e lastro documental.");
            confidence -= 0.06d;
        }

        String status = alerts.isEmpty() ? "REQUEST_GOVERNANCE_STABLE" : "REQUEST_GOVERNANCE_ATTENTION";
        return new InstitutionalGovernanceContextReport(
                "PETITION_ASSIST",
                status,
                round(clamp(confidence)),
                List.copyOf(anchors),
                List.copyOf(alerts),
                List.copyOf(guards),
                List.copyOf(escalation),
                List.copyOf(keys),
                PayloadMaps.ofEntries(
                        "scope", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "tribunal", canonicalTribunalCodigo,
                        "classeTpu", canonicalClasseTpu,
                        "coherenceBlocking", coherence != null && coherence.blocking()
                )
        );
    }

    public InstitutionalGovernanceContextReport analyzeProcess(Processo processo,
                                                               String ritoName,
                                                               SettlementAdvisoryReport settlement,
                                                               InstitutionalMemoryReport memory,
                                                               ContextualPrecedentAdvisoryReport precedents) {
        Objects.requireNonNull(processo, "processo");
        Set<String> anchors = new LinkedHashSet<>();
        Set<String> alerts = new LinkedHashSet<>();
        Set<String> guards = new LinkedHashSet<>();
        Set<String> escalation = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        double confidence = 0.7d;

        Long processoId = processo.getId();
        String faseAtual = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null;
        String jurisdicaoNome = processo.getJurisdicao() != null ? processo.getJurisdicao().getNome() : null;
        String ramoDireito = processo.getRamoDireito() == null ? null : processo.getRamoDireito().name();

        addWhen(anchors, processoId != null, "processo:" + processoId);
        addWhen(anchors, !blank(processo.getNumeroUnificado()), "numero:" + processo.getNumeroUnificado());
        addWhen(anchors, !blank(ritoName), "rito:" + ritoName);
        addWhen(anchors, !blank(faseAtual), "fase:" + faseAtual);
        addWhen(keys, !blank(jurisdicaoNome), "governance:foro:" + jurisdicaoNome);
        addWhen(keys, !blank(ramoDireito), "governance:ramo:" + ramoDireito);

        guards.add("Sincronizar operação do processo com rito efetivo, fase atual e salvaguardas de execução.");
        guards.add("Usar governança institucional para distinguir automação segura de decisão que exige revisão humana.");

        if (settlement != null) {
            guards.addAll(limit(settlement.executionSafeguards(), 3));
            escalation.addAll(limit(settlement.conditionalClauses(), 2));
            confidence += settlement.executable() ? 0.03d : -0.08d;
        }

        if (memory != null) {
            guards.addAll(limit(memory.reusablePlaybooks(), 3));
            alerts.addAll(limit(memory.officeAlerts(), 3));
            escalation.addAll(limit(memory.repeatedFailureModes(), 2));
            confidence += memory.repeatedFailureModes().isEmpty() ? 0.03d : -0.04d;
        }

        if (precedents != null) {
            anchors.addAll(limit(precedents.anchorDimensions(), 3));
            guards.addAll(limit(precedents.targetDecisionProfiles(), 2));
            alerts.addAll(limit(precedents.cautionPoints(), 3));
            confidence += precedents.cautionPoints().isEmpty() ? 0.02d : -0.03d;
        }

        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            guards.add("Fluxo sigiloso exige governança reforçada, segmentação de acesso e rastreabilidade institucional.");
            alerts.add("Manter memória institucional do caso com filtragem compatível com credencial.");
            confidence -= 0.03d;
        }

        String status = alerts.isEmpty() ? "PROCESS_GOVERNANCE_STABLE" : "PROCESS_GOVERNANCE_ATTENTION";
        return new InstitutionalGovernanceContextReport(
                "PROCESS_TWIN",
                status,
                round(clamp(confidence)),
                List.copyOf(anchors),
                List.copyOf(alerts),
                List.copyOf(guards),
                List.copyOf(escalation),
                List.copyOf(keys),
                PayloadMaps.ofEntries(
                        "scope", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "executability", settlement != null && settlement.executable()
                )
        );
    }

    private static void addWhen(Set<String> target, boolean condition, String value) {
        if (condition && !blank(value)) {
            target.add(value);
        }
    }

    private static boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static List<String> limit(List<String> source, int max) {
        if (source == null || source.isEmpty() || max <= 0) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
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
