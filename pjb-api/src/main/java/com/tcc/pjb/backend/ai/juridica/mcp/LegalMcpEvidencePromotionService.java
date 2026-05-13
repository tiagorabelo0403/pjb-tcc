package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDoctorReport;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpEvidencePromotionDecision;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionTranscript;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolExample;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpEvidencePromotionService {

    public LegalMcpEvidencePromotionDecision resolve(LegalMcpServerProfile.ResolveRequest request,
                                                     LegalEvalReplayResult evaluation,
                                                     LegalMcpExecutionTranscript transcript,
                                                     LegalMcpDoctorReport doctor,
                                                     List<LegalMcpToolExample> examples) {
        Objects.requireNonNull(transcript, "transcript");
        List<LegalMcpToolExample> safeExamples = examples == null ? List.of() : List.copyOf(examples);
        LinkedHashSet<String> promotedToolExampleIds = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        List<String> safeguards = new ArrayList<>();
        double qualityScore = evaluation == null ? 0.0d : evaluation.qualityScore();
        boolean replayReady = transcript.replayReady();
        boolean doctorReady = doctor != null && doctor.ready();
        boolean promptInjection = request != null && request.promptInjectionDetected();
        boolean quarantined = request != null && request.quarantinedContext();
        boolean sigilo = request != null && request.sigilo();

        for (LegalMcpToolExample example : safeExamples) {
            if (example == null || example.exampleId() == null) {
                continue;
            }
            if (!transcript.pinnedToolExampleIds().contains(example.exampleId())) {
                continue;
            }
            if (!replayReady || !doctorReady || qualityScore < 85.0d) {
                continue;
            }
            if (promptInjection || quarantined) {
                continue;
            }
            if (sigilo && (example.toolId().startsWith("interoperability.") || example.toolId().startsWith("precedent.")) && qualityScore < 92.0d) {
                continue;
            }
            promotedToolExampleIds.add(example.exampleId());
        }

        String status;
        if (promotedToolExampleIds.isEmpty()) {
            status = safeExamples.isEmpty() ? "NO_ELIGIBLE_EXAMPLES" : "PROMOTION_HELD";
        } else {
            status = "PROMOTED_FROM_REPLAY";
        }

        String approvalLane;
        if (promptInjection || quarantined || (doctor != null && "BLOCKED".equalsIgnoreCase(doctor.status()))) {
            approvalLane = "HUMAN_REVIEW_REQUIRED";
            safeguards.add("EVIDENCE_PROMOTION_HUMAN_REVIEW");
        } else if (sigilo || transcript.approvalLinked() || qualityScore < 85.0d) {
            approvalLane = "STEP_UP_REQUIRED";
            safeguards.add("EVIDENCE_PROMOTION_STEP_UP");
        } else {
            approvalLane = "AUTO_READONLY_MONITORED";
            safeguards.add("EVIDENCE_PROMOTION_MONITORED");
        }

        reasons.add("replayReady=" + replayReady);
        reasons.add("doctorReady=" + doctorReady);
        reasons.add("qualityScore=" + qualityScore);
        reasons.add("promotedExamples=" + promotedToolExampleIds.size());
        if (sigilo) {
            reasons.add("sigilo_chain_requires_lane_control");
        }
        if (promptInjection) {
            reasons.add("prompt_injection_freezes_example_promotion");
        }
        if (quarantined) {
            reasons.add("quarantined_context_restricts_promotions");
        }

        return new LegalMcpEvidencePromotionDecision(
                "LEGAL_MCP_EVIDENCE_PROMOTION_" + UUID.randomUUID(),
                status,
                replayReady,
                qualityScore,
                approvalLane,
                List.copyOf(promotedToolExampleIds),
                List.copyOf(reasons),
                List.copyOf(safeguards)
        );
    }
}
