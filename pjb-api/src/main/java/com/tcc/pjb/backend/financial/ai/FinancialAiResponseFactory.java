package com.tcc.pjb.backend.financial.ai;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinancialAiResponseFactory {

    private final Clock pjbClock;

    public FinancialAiResponseFactory(Clock pjbClock) {
        this.pjbClock = Objects.requireNonNull(pjbClock);
    }

    public FinancialAiDescriptor descriptor(ApiVersion version) {
        ApiVersion resolved = version != null ? version : ApiVersion.latest();
        Instant builtAt = Instant.now(pjbClock);
        return switch (resolved) {
            case V1 -> new FinancialAiDescriptor(
                    "FINANCIAL_AI",
                    ApiVersion.V1,
                    "Financial AI consolidada com estimativa deterministica e contrato unificado.",
                    baseCapabilities(),
                    builtAt
            );
            case V2 -> new FinancialAiDescriptor(
                    "FINANCIAL_AI",
                    ApiVersion.V2,
                    "Financial AI consolidada com matriz de risco operacional e contrato unificado.",
                    baseCapabilities(),
                    builtAt
            );
            case V3 -> new FinancialAiDescriptor(
                    "FINANCIAL_AI",
                    ApiVersion.V3,
                    "Financial AI consolidada com explainability, risco e contrato unificado.",
                    v3Capabilities(),
                    builtAt
            );
        };
    }

    public FinancialAiResponse from(IARequest request, IAResponse response, ApiVersion version) {
        ApiVersion resolved = version != null ? version : ApiVersion.latest();
        return FinancialAiLegacyResponseMapper.toUnified(request, response, resolved, descriptor(resolved));
    }

    public Map<String, Object> envelope(IARequest request, IAResponse response, ApiVersion version) {
        FinancialAiResponse unified = from(request, response, version);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("financial_ai", FinancialAiLegacyResponseMapper.toEnvelope(unified));
        envelope.put("financial_ai_descriptor", FinancialAiLegacyResponseMapper.toDescriptorEnvelope(descriptor(unified.version())));
        return envelope;
    }

    private static Set<String> baseCapabilities() {
        return orderedCapabilitySet(Capability.PAYMENTS, Capability.INTEREST, Capability.CORRECTION, Capability.RISK, Capability.SETTLEMENT, Capability.AUDIT);
    }

    private static Set<String> v3Capabilities() {
        EnumSet<Capability> capabilities = EnumSet.allOf(Capability.class);
        return orderedCapabilitySet(capabilities.toArray(Capability[]::new));
    }

    private static Set<String> orderedCapabilitySet(Capability... capabilities) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (Capability capability : capabilities) {
            if (capability != null) {
                ordered.add(capability.wire());
            }
        }
        return Collections.unmodifiableSet(ordered);
    }

    private enum Capability {
        PAYMENTS("payments"),
        INTEREST("interest"),
        CORRECTION("correction"),
        RISK("risk"),
        SETTLEMENT("settlement"),
        AUDIT("audit"),
        EXPLAIN("explain");

        private final String wire;

        Capability(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }
}
