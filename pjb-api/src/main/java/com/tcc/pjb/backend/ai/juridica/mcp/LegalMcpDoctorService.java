package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpContextCompactionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDeliberationPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDoctorCheck;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDoctorReport;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpDoctorService {

    public LegalMcpDoctorReport inspect(LegalMcpServerProfile.ResolveRequest request,
                                        LegalMcpExecutionPlan plan,
                                        LegalEvalReplayResult evaluation,
                                        LegalMcpDeliberationPlan deliberation,
                                        LegalMcpContextCompactionPlan contextCompaction) {
        Objects.requireNonNull(plan, "plan");
        List<LegalMcpDoctorCheck> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> blockers = new ArrayList<>();

        checks.add(check(
                "MCP_SERVER_PINNING",
                "Pinagem mínima de servidores",
                plan.pinnedServers() != null && !plan.pinnedServers().isEmpty(),
                true,
                plan.pinnedServers() == null || plan.pinnedServers().isEmpty()
                        ? "Nenhum servidor MCP elegível foi fixado."
                        : "Servidores fixados: " + plan.pinnedServers().stream().map(server -> server.serverId()).toList()
        ));

        boolean evaluationOk = evaluation != null && evaluation.qualityScore() >= 75.0d;
        checks.add(check(
                "MCP_EVAL_SCORE",
                "Score mínimo do benchmark",
                evaluationOk,
                false,
                evaluation == null
                        ? "Plano sem replay benchmarkado."
                        : "qualityScore=" + evaluation.qualityScore() + ", passed=" + evaluation.passed()
        ));
        if (!evaluationOk) {
            warnings.add("Plano MCP abaixo do alvo de qualidade para operação livre.");
        }

        boolean deliberationOk = deliberation == null || !deliberation.required() || (deliberation.mode() != null && !deliberation.mode().isBlank());
        checks.add(check(
                "MCP_DELIBERATION_LINK",
                "Checkpoint deliberativo amarrado",
                deliberationOk,
                request != null && (request.sigilo() || request.promptInjectionDetected() || request.quarantinedContext()),
                deliberation == null ? "Deliberação não exigida." : "mode=" + deliberation.mode() + ", required=" + deliberation.required()
        ));
        if (!deliberationOk) {
            blockers.add("Checkpoint deliberativo inconsistente para contexto sensível.");
        }

        boolean compactionOk = contextCompaction != null && contextCompaction.retainedHistoryBudget() >= 0;
        checks.add(check(
                "MCP_CONTEXT_COMPACTION",
                "Compactação de contexto definida",
                compactionOk,
                false,
                contextCompaction == null ? "Política de compactação ausente." : "policy=" + contextCompaction.policy() + ", retainedHistoryBudget=" + contextCompaction.retainedHistoryBudget()
        ));
        if (!compactionOk) {
            warnings.add("Plano sem política explícita de compactação.");
        }

        boolean noUnsafeTransport = plan.transportProfile() != null && !plan.transportProfile().isBlank();
        checks.add(check(
                "MCP_TRANSPORT_GOVERNANCE",
                "Transporte governado",
                noUnsafeTransport,
                true,
                "transportProfile=" + plan.transportProfile()
        ));
        if (!noUnsafeTransport) {
            blockers.add("Plano MCP sem perfil de transporte governado.");
        }

        String status = !blockers.isEmpty() ? "BLOCKED" : !warnings.isEmpty() ? "DEGRADED" : "READY";
        boolean ready = blockers.isEmpty();
        String operationalMode = !ready ? "STRICT_REVIEW" : warnings.isEmpty() ? "AUTO_READONLY_MONITORED" : "MONITORED_WITH_REVIEW_HINTS";
        return new LegalMcpDoctorReport(
                "LEGAL_MCP_DOCTOR_" + UUID.randomUUID(),
                status,
                ready,
                operationalMode,
                List.copyOf(checks),
                List.copyOf(warnings),
                List.copyOf(blockers)
        );
    }

    private LegalMcpDoctorCheck check(String checkId,
                                      String label,
                                      boolean passed,
                                      boolean blocking,
                                      String details) {
        return new LegalMcpDoctorCheck(
                checkId,
                label,
                passed ? "PASS" : blocking ? "FAIL" : "WARN",
                blocking && !passed,
                details
        );
    }
}
