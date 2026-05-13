package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalProcessualMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_PROCESSUAL",
                "MCP Processual e Competência",
                "PROCESSUAL",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "SERVER_MANAGED_OAUTH2_1",
                true,
                true,
                true,
                false,
                "PROCEDURAL_COMPATIBILITY_CHAIN",
                List.of("rito_compatibility_probe", "cabimento_matrix", "competence_route_explainer"),
                List.of("taxonomy://tpu", "processual://cabimento", "processual://competencia"),
                List.of("PROCESSUAL", "COMPETENCIA", "CABIMENTO", "SIGILO"),
                List.of(
                        tool("processual.competence.route", "Roteamento de competência", "READ_ONLY_COMPETENCE_ROUTING", false, "COMPETENCE_MATRIX", "LOW"),
                        tool("processual.cabimento.check", "Checagem de cabimento", "READ_ONLY_CABIMENTO", false, "PROCEDURAL_COMPATIBILITY", "LOW"),
                        tool("processual.rito.compatibility", "Compatibilidade procedimental", "READ_ONLY_RITO_CHECK", false, "PROCEDURAL_COMPATIBILITY", "LOW")
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
        int score = 20;
        
        if (capability.contains("PROTOCOLO") || capability.contains("PETICAO") || capability.contains("COMPETENCIA") || capability.contains("VALIDATE")) score += 10;
        if (schema.contains("PROCEDURAL_PLAN") || schema.contains("CHECKLIST") || schema.contains("DRAFT")) score += 7;
        if (rito.contains("JUIZADO") || rito.contains("COMUM") || rito.contains("ESPECIAL") || rito.contains("RECURSAL")) score += 5;
        if (ramo.contains("PENAL") || ramo.contains("CIVIL") || ramo.contains("TRABALH") || ramo.contains("ELEITORAL")) score += 4;
        if (request.sigilo()) score += 4;
        if (request.promptInjectionDetected()) score -= 1;
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
