package com.tcc.pjb.backend.ai.financeira.router;

import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.financeira.v1.IAFinanceiraV1;
import com.tcc.pjb.backend.ai.financeira.v2.IAFinanceiraV2;
import com.tcc.pjb.backend.ai.financeira.v3.IAFinanceiraV3;
import com.tcc.pjb.backend.financial.ai.FinancialAiDescriptor;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponse;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponseFactory;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.platform.versioning.VersionHints;
import com.tcc.pjb.backend.platform.versioning.VersionedCapabilityRegistry;

public final class FinanceiraAiVersionSelector {

    private final VersionedCapabilityRegistry<IAService> registry;
    private final FinancialAiResponseFactory responseFactory;

    public FinanceiraAiVersionSelector(IAFinanceiraV1 v1, IAFinanceiraV2 v2, IAFinanceiraV3 v3, FinancialAiResponseFactory responseFactory) {
        Objects.requireNonNull(v1, "v1");
        Objects.requireNonNull(v2, "v2");
        Objects.requireNonNull(v3, "v3");
        this.responseFactory = Objects.requireNonNull(responseFactory, "responseFactory");

        this.registry = new VersionedCapabilityRegistry<IAService>()
                .register(ApiVersion.V1, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v1)
                .register(ApiVersion.V2, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v2)
                .register(ApiVersion.V3, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v3);
    }

    public IAService resolve(IARequest request) {
        return resolve(request, null, null);
    }

    public IAService resolve(IARequest request, ApiVersion forcedVersion, String forcedCapability) {
        ApiVersion version = forcedVersion != null ? forcedVersion : resolveVersion(request);
        String capability = forcedCapability != null && !forcedCapability.isBlank() ? forcedCapability : resolveCapability(request);
        return registry.resolveWithFallback(version, capability)
                .orElseThrow(() -> new IllegalStateException("Nenhuma IA Financeira registrada para version=" + version + ", capability=" + capability));
    }

    public IAResponse process(IARequest request) {
        return process(request, null, null);
    }

    public IAResponse process(IARequest request, ApiVersion forcedVersion, String forcedCapability) {
        return resolve(request, forcedVersion, forcedCapability).processar(request);
    }

    public FinancialAiResponse processUnified(IARequest request) {
        ApiVersion version = resolveVersion(request);
        IAResponse raw = process(request, version, resolveCapability(request));
        return responseFactory.from(request, raw, version);
    }

    public FinancialAiResponse processUnified(IARequest request, ApiVersion forcedVersion, String forcedCapability) {
        ApiVersion version = forcedVersion != null ? forcedVersion : resolveVersion(request);
        IAResponse raw = process(request, version, forcedCapability);
        return responseFactory.from(request, raw, version);
    }

    public FinancialAiDescriptor descriptor(ApiVersion version) {
        return responseFactory.descriptor(version != null ? version : ApiVersion.latest());
    }

    public ApiVersion resolveVersion(IARequest request) {
        Map<String, Object> payload = request != null ? request.getPayload() : null;
        return VersionHints.resolveVersion(payload, ApiVersion.latest());
    }

    public String resolveCapability(IARequest request) {
        Map<String, Object> payload = request != null ? request.getPayload() : null;
        String fallback = request != null ? request.getAcao() : null;
        return VersionHints.resolveCapability(payload, fallback);
    }
}
