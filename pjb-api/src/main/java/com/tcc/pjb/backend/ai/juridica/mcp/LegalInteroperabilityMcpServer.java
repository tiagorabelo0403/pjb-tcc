package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalInteroperabilityMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_INTEROPERABILIDADE",
                "MCP Interoperabilidade Judicial",
                "INTEROPERABILIDADE",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "SERVER_MANAGED_OAUTH2_1",
                true,
                true,
                true,
                false,
                "FEDERATED_ACCESS_CHAIN",
                List.of("legacy_discovery_fence", "federated_access_capability", "sync_delta_projection"),
                List.of("legacy://pje", "legacy://esaj", "legacy://eproc", "legacy://projudi"),
                List.of("INTEROPERABILIDADE", "LEGADOS", "DISCOVERY", "SYNC"),
                List.of(
                        tool("interop.legacy.discovery", "Descoberta federada", "READ_ONLY_LEGACY_DISCOVERY", true, "LEGACY_DISCOVERY", "MEDIUM"),
                        tool("interop.case.access.capability", "Capacidade de acesso federado", "READ_ONLY_LEGACY_ACCESS", true, "LEGACY_ACCESS", "HIGH"),
                        tool("interop.sync.delta.plan", "Plano de sincronização incremental", "READ_ONLY_SYNC_DELTA", true, "LEGACY_SYNC", "HIGH")
                )
        );
    }

    @Override
    public int score(ResolveRequest request) {
        String capability = normalize(request.capability());
        String ramo = normalize(request.ramo());
        String rito = normalize(request.rito());
        String profile = normalize(request.userProfile());
        String schema = request.recommendedSchema() == null ? "" : normalize(request.recommendedSchema().schemaId());
        int score = 10;
        
        if (capability.contains("INTEROPER") || capability.contains("DISCOVERY") || capability.contains("ACESSO")) score += 12;
        if (schema.contains("RISK") || schema.contains("CHECKLIST")) score += 4;
        if (containsHistory(request.history(), "pje", "e-saj", "esaj", "eproc", "creta", "projudi", "seeu")) score += 12;
        if (containsContext(request.context(), "sourceSystem", "legacySystem", "tribunalSistema")) score += 8;
        if (request.sigilo()) score += 6;
        if (request.promptInjectionDetected()) score -= 4;
        if (request.quarantinedContext()) score -= 6;
        return Math.max(score, 0);
    }

    private LegalMcpToolDescriptor tool(String toolId,
                                        String label,
                                        String toolClass,
                                        boolean requiresStepUp,
                                        String evidenceLane,
                                        String riskLevel) {
        return new LegalMcpToolDescriptor(
                toolId,
                label,
                toolClass,
                true,
                requiresStepUp,
                evidenceLane,
                riskLevel,
                Map.of(
                        "readOnlyHint", true,
                        "destructiveHint", false,
                        "idempotentHint", true,
                        "openWorldHint", false
                )
        );
    }


    private boolean containsHistory(List<String> history, String... terms) {
        if (history == null || history.isEmpty()) return false;
        String joined = normalize(String.join(" ", history));
        for (String term : terms) {
            if (!normalize(term).isBlank() && joined.contains(normalize(term))) return true;
        }
        return false;
    }

    private boolean containsContext(Map<String, Object> context, String... keys) {
        if (context == null || context.isEmpty()) return false;
        for (String key : keys) {
            Object value = context.get(key);
            if (value != null && !normalize(String.valueOf(value)).isBlank()) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
