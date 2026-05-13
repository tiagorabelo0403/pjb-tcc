package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpTextCatalogService;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDeliberationPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpSkillDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolExample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpDeliberationCheckpointService {

    private final LegalMcpTextCatalogService textCatalogService;

    public LegalMcpDeliberationCheckpointService(LegalMcpTextCatalogService textCatalogService) {
        this.textCatalogService = Objects.requireNonNull(textCatalogService, "textCatalogService");
    }

    public LegalMcpDeliberationPlan resolve(LegalMcpServerProfile.ResolveRequest request,
                                            LegalEvalReplayResult evaluation,
                                            List<LegalMcpSkillDescriptor> skills,
                                            List<LegalMcpToolExample> examples) {
        List<String> reasons = new ArrayList<>();
        boolean required = false;
        if (request.sigilo()) {
            reasons.add(textCatalogService.deliberationReasonSigiloStrong());
            required = true;
        }
        if (request.promptInjectionDetected() || request.quarantinedContext()) {
            reasons.add(textCatalogService.deliberationReasonPromptInjectionOrQuarantine());
            required = true;
        }
        if (textCatalogService.isHighImpactCapability(request.capability())) {
            reasons.add(textCatalogService.deliberationReasonHighImpactTask());
            required = true;
        }
        if (evaluation != null && !evaluation.passed()) {
            reasons.add(textCatalogService.deliberationReasonBenchmarkBelowExpected());
            required = true;
        }
        String mode = resolveMode(request, required);
        return new LegalMcpDeliberationPlan(
                required,
                mode,
                required ? textCatalogService.deliberationCheckpointPrefix() + UUID.randomUUID() : null,
                List.copyOf(reasons),
                skills == null ? List.of() : skills.stream().map(LegalMcpSkillDescriptor::skillId).toList(),
                examples == null ? List.of() : examples.stream().map(LegalMcpToolExample::exampleId).toList()
        );
    }

    private String resolveMode(LegalMcpServerProfile.ResolveRequest request, boolean required) {
        if (!required) {
            return textCatalogService.deliberationModeInlineFastPath();
        }
        if (request.promptInjectionDetected() || request.quarantinedContext()) {
            return textCatalogService.deliberationModeIsolatedPolicyReview();
        }
        if (request.sigilo()) {
            return textCatalogService.deliberationModeSignedAuthorityRecheck();
        }
        return textCatalogService.deliberationModeThinkToolStyleCheckpoint();
    }
}
