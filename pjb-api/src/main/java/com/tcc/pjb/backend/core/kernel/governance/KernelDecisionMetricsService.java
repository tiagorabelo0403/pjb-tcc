package com.tcc.pjb.backend.core.kernel.governance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.KernelDecisionEventRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KernelDecisionMetricsService {

    private final KernelDecisionEventRepository repository;

    public KernelDecisionMetricsReport analyzeProcess(Processo processo) {
        long total = repository.countByProcessoId(processo.getId());
        long blocked = repository.countByProcessoIdAndReleaseAllowedFalse(processo.getId());
        long approval = repository.countByProcessoIdAndApprovalRequiredTrue(processo.getId());
        long draft = repository.countByProcessoIdAndInternalDraftRequiredTrue(processo.getId());
        long last24h = repository.countByProcessoIdAndDataCriacaoAfter(processo.getId(), LocalDateTime.now().minusHours(24));

        Set<String> hotSignals = new LinkedHashSet<>();
        Set<String> stabilitySignals = new LinkedHashSet<>();
        double confidence = total == 0 ? 0.58d : 0.78d;

        if (total == 0) {
            hotSignals.add("Ainda não existe histórico de decisão suficiente para inferência estatística forte do kernel.");
        } else {
            stabilitySignals.add("O processo já acumula telemetria institucional suficiente para comparar rodadas negociais.");
        }
        if (blocked > 0) {
            hotSignals.add("O kernel registrou bloqueios de liberação que precisam ser entendidos antes de acelerar o canal.");
            confidence -= 0.04d;
        }
        if (approval > 0) {
            hotSignals.add("Há exigência recorrente de aprovação interna ou externa no fluxo negocial deste processo.");
        }
        if (draft > 0) {
            hotSignals.add("O histórico mostra dependência de rascunho interno antes de comunicação externa segura.");
        }
        if (last24h == 0 && total > 0) {
            stabilitySignals.add("Não houve oscilação operacional recente do kernel nas últimas 24 horas.");
        }
        if (blocked == 0 && approval == 0 && total > 0) {
            stabilitySignals.add("A série recente do kernel não registrou bloqueio ou exigência extraordinária de alçada.");
            confidence += 0.06d;
        }

        String status = blocked > 0 || approval > 1 ? "KERNEL_METRICS_ATTENTION" : "KERNEL_METRICS_STABLE";
        return new KernelDecisionMetricsReport(
                "KERNEL_DECISION_METRICS",
                status,
                round(clamp(confidence)),
                total,
                blocked,
                approval,
                draft,
                last24h,
                List.copyOf(hotSignals),
                List.copyOf(stabilitySignals),
                PayloadMaps.ofEntries(
                        "processoId", processo.getId(),
                        "totalDecisions", total,
                        "blockedDecisions", blocked,
                        "approvalRequiredDecisions", approval,
                        "internalDraftDecisions", draft,
                        "last24hDecisions", last24h
                )
        );
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(0.99d, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
