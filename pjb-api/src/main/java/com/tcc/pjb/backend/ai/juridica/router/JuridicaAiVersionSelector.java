package com.tcc.pjb.backend.ai.juridica.router;

import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.juridica.v1.IAJuridicaV1;
import com.tcc.pjb.backend.ai.juridica.v2.IAJuridicaV2;
import com.tcc.pjb.backend.ai.juridica.v3.IAJuridicaV3;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.platform.versioning.VersionHints;
import com.tcc.pjb.backend.platform.versioning.VersionedCapabilityRegistry;

public final class JuridicaAiVersionSelector {

    private final VersionedCapabilityRegistry<IAService> registry;

    public JuridicaAiVersionSelector(IAJuridicaV1 v1, IAJuridicaV2 v2, IAJuridicaV3 v3) {
        Objects.requireNonNull(v1, "v1");
        Objects.requireNonNull(v2, "v2");
        Objects.requireNonNull(v3, "v3");

        this.registry = new VersionedCapabilityRegistry<IAService>()
                .register(ApiVersion.V1, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v1)
                .register(ApiVersion.V2, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v2)
                .register(ApiVersion.V3, VersionedCapabilityRegistry.DEFAULT_CAPABILITY, v3);
    }

    public IAService resolve(IARequest request) {
        ApiVersion version = resolveVersion(request);
        String capability = resolveCapability(request);
        return registry.resolveWithFallback(version, capability)
                .orElseThrow(() -> new IllegalStateException("Nenhuma IA Jurídica registrada para version=" + version + ", capability=" + capability));
    }

    public IAResponse process(IARequest request) {
        return resolve(request).processar(request);
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
