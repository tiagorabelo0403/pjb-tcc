package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalJurisprudenceMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_JURISPRUDENCIA",
                "MCP Jurisprudência Estratégica",
                "JURISPRUDENCIA",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "OPTIONAL_PUBLIC_READONLY",
                true,
                true,
                true,
                false,
                "PRECEDENT_LINEAGE_CHAIN",
                List.of("precedent_lineage_trace", "contradiction_fence", "authority_floor_recheck"),
                List.of("precedents://stf", "precedents://stj", "themes://rg_repetitivos"),
                List.of("JURISPRUDENCIA", "PRECEDENTES", "TEMAS", "CONTRADICAO"),
                List.of(
                        tool("precedent.search", "Busca de precedentes", "READ_ONLY_SEARCH", false, "PRECEDENT_TEXT", "LOW"),
                        tool("precedent.theme.lookup", "Consulta de temas", "READ_ONLY_PRECEDENT_LOOKUP", false, "PRECEDENT_THEME", "LOW"),
                        tool("precedent.lineage.trace", "Rastreio de linhagem", "READ_ONLY_PRECEDENT_LINEAGE", false, "AUTHORITY_FLOOR", "LOW")
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
        int score = 17;
        
        if (capability.contains("CONSULTA") || capability.contains("PARECER") || capability.contains("PETICAO") || capability.contains("RESEARCH")) score += 8;
        if (schema.contains("RISK") || schema.contains("PARECER") || schema.contains("DRAFT")) score += 5;
        if (ramo.contains("CIVIL") || ramo.contains("PENAL") || ramo.contains("TRABALH") || ramo.contains("FAMILIA") || ramo.contains("FAZENDA")) score += 4;
        if (containsHistory(request.history(), "precedente", "stj", "stf", "tema", "sumula")) score += 7;
        if (request.sigilo()) score += 0;
        if (request.promptInjectionDetected()) score -= 2;
        if (request.quarantinedContext()) score -= 1;
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
