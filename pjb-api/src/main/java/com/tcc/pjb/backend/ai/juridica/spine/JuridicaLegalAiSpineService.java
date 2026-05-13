package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaLegalAiSpineService {

    private final JuridicaPolicyVariableService policyVariableService;
    private final JuridicaToolRoutingService toolRoutingService;
    private final JuridicaStructuredOutputProfileService structuredOutputProfileService;
    private final JuridicaHybridRetrievalProfileService retrievalProfileService;
    private final JuridicaMemoryIsolationProfileService memoryIsolationProfileService;
    private final JuridicaSymbolicValidationProfileService validationProfileService;
    private final JuridicaGraphProfileService graphProfileService;
    private final JuridicaMultimodalProfileService multimodalProfileService;
    private final JuridicaEvaluationProfileService evaluationProfileService;
    private final JuridicaAntiHallucinationProfileService antiHallucinationProfileService;
    private final JuridicaTraceApprovalService traceApprovalService;

    public JuridicaLegalAiSpineService(JuridicaPolicyVariableService policyVariableService,
                                       JuridicaToolRoutingService toolRoutingService,
                                       JuridicaStructuredOutputProfileService structuredOutputProfileService,
                                       JuridicaHybridRetrievalProfileService retrievalProfileService,
                                       JuridicaMemoryIsolationProfileService memoryIsolationProfileService,
                                       JuridicaSymbolicValidationProfileService validationProfileService,
                                       JuridicaGraphProfileService graphProfileService,
                                       JuridicaMultimodalProfileService multimodalProfileService,
                                       JuridicaEvaluationProfileService evaluationProfileService,
                                       JuridicaAntiHallucinationProfileService antiHallucinationProfileService,
                                       JuridicaTraceApprovalService traceApprovalService) {
        this.policyVariableService = Objects.requireNonNull(policyVariableService, "policyVariableService");
        this.toolRoutingService = Objects.requireNonNull(toolRoutingService, "toolRoutingService");
        this.structuredOutputProfileService = Objects.requireNonNull(structuredOutputProfileService, "structuredOutputProfileService");
        this.retrievalProfileService = Objects.requireNonNull(retrievalProfileService, "retrievalProfileService");
        this.memoryIsolationProfileService = Objects.requireNonNull(memoryIsolationProfileService, "memoryIsolationProfileService");
        this.validationProfileService = Objects.requireNonNull(validationProfileService, "validationProfileService");
        this.graphProfileService = Objects.requireNonNull(graphProfileService, "graphProfileService");
        this.multimodalProfileService = Objects.requireNonNull(multimodalProfileService, "multimodalProfileService");
        this.evaluationProfileService = Objects.requireNonNull(evaluationProfileService, "evaluationProfileService");
        this.antiHallucinationProfileService = Objects.requireNonNull(antiHallucinationProfileService, "antiHallucinationProfileService");
        this.traceApprovalService = Objects.requireNonNull(traceApprovalService, "traceApprovalService");
    }

    public LegalAiSpineProfileResponse resolveForIa(IARequest request, ApiVersion version, String capability) {
        Map<String, Object> payload = request != null && request.getPayload() != null ? request.getPayload() : Map.of();
        return resolve(version, capability, payload);
    }

    public LegalAiSpineProfileResponse resolveForSurface(String capability, ApiVersion version) {
        return resolve(version, capability, Map.of());
    }

    public LegalAiSpineProfileResponse resolveForSkill(String capability, ApiVersion version, Map<String, Object> payload) {
        return resolve(version, capability, payload == null ? Map.of() : payload);
    }

    private LegalAiSpineProfileResponse resolve(ApiVersion version, String capability, Map<String, Object> payload) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        String normalizedCapability = capability == null || capability.isBlank() ? "LEGAL_GENERAL_ASSIST_" + effectiveVersion.name() : capability.trim().toUpperCase(Locale.ROOT);
        Map<String, Object> policyVariables = policyVariableService.resolve(effectiveVersion, normalizedCapability, payload);
        List<com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor> tools = toolRoutingService.resolve(effectiveVersion, normalizedCapability, payload);
        var outputs = structuredOutputProfileService.resolve(effectiveVersion);
        var retrieval = retrievalProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var memory = memoryIsolationProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var validation = validationProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var graph = graphProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var multimodal = multimodalProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var evaluation = evaluationProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var hallucinationGuard = antiHallucinationProfileService.resolve(effectiveVersion, normalizedCapability, policyVariables);
        var trace = traceApprovalService.trace(effectiveVersion, normalizedCapability, policyVariables, tools);
        var approval = traceApprovalService.approval(effectiveVersion, normalizedCapability, policyVariables, tools);
        return new LegalAiSpineProfileResponse(
                JuridicaSpineLabels.PROFILE_LEGAL_SPINE,
                effectiveVersion.name(),
                normalizedCapability,
                policyVariables,
                outputs,
                tools,
                retrieval,
                memory,
                validation,
                graph,
                multimodal,
                evaluation,
                hallucinationGuard,
                trace,
                approval
        );
    }
}
