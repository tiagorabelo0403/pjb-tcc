package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalLegislationMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_LEGISLACAO",
                "MCP Legislação Canônica",
                "LEGISLACAO",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "OPTIONAL_PUBLIC_READONLY",
                true,
                true,
                true,
                false,
                "NORMATIVE_AUTHORITY_CHAIN",
                List.of("citation_first_normative_trace", "vigencia_temporal_probe", "competencia_lex_specialis"),
                List.of("constitution://cf88", "codes://cpc", "codes://cpp", "laws://special"),
                List.of("LEGISLACAO", "VIGENCIA", "FUNDAMENTO_NORMATIVO", "CITATION_FIRST"),
                List.of(
                        tool("norma.search", "Busca normativa", "READ_ONLY_LEGAL_LOOKUP", false, "NORMATIVE_TEXT", "LOW"),
                        tool("norma.article.lookup", "Consulta por artigo", "READ_ONLY_LEGAL_LOOKUP", false, "NORMATIVE_TEXT", "LOW"),
                        tool("norma.vigencia.check", "Checagem de vigência", "READ_ONLY_TEMPORALITY", false, "TEMPORAL_VALIDITY", "LOW")
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
        int score = 18;
        
        if (capability.contains("PETICAO") || capability.contains("PARECER") || capability.contains("LEGAL")) score += 8;
        if (capability.contains("PROTOCOLO") || capability.contains("DECISAO")) score += 4;
        if (schema.contains("PARECER") || schema.contains("DRAFT") || schema.contains("DESPACHO") || schema.contains("DECISAO")) score += 6;
        if (ramo.contains("TRIBUT") || ramo.contains("PREVID") || ramo.contains("PENAL") || ramo.contains("AMBIENTAL")) score += 4;
        if (profile.contains("MAGISTRADO") || profile.contains("PROCURADOR") || profile.contains("DEFENSOR")) score += 2;
        if (request.sigilo()) score += 1;
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
