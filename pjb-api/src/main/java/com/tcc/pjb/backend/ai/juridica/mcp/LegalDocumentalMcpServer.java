package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalDocumentalMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_DOCUMENTAL",
                "MCP Documental e Proveniência",
                "DOCUMENTAL",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "SERVER_MANAGED_OAUTH2_1",
                true,
                true,
                true,
                false,
                "PROVENANCE_AND_SIGNATURE_CHAIN",
                List.of("document_provenance_fence", "signature_integrity_probe", "attachment_manifest_guard"),
                List.of("documents://manifest", "documents://signatures", "documents://provenance"),
                List.of("DOCUMENTAL", "ASSINATURA", "PROVENIENCIA", "ANEXOS"),
                List.of(
                        tool("document.signature.verify", "Verificação de assinatura", "READ_ONLY_SIGNATURE_VERIFY", true, "SIGNATURE_EVIDENCE", "MEDIUM"),
                        tool("document.provenance.inspect", "Inspeção de proveniência", "READ_ONLY_PROVENANCE", true, "PROVENANCE", "MEDIUM"),
                        tool("document.attachment.manifest", "Manifesto de anexos", "READ_ONLY_DOCUMENT_MANIFEST", false, "DOCUMENT_MANIFEST", "LOW")
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
        int score = 14;
        
        if (capability.contains("PROTOCOLO") || capability.contains("DOCUMENT") || capability.contains("PETICAO") || capability.contains("VALIDATE")) score += 8;
        if (schema.contains("DRAFT") || schema.contains("DESPACHO") || schema.contains("DECISAO")) score += 3;
        if (request.attachments() != null && !request.attachments().isEmpty()) score += 12;
        if (containsHistory(request.history(), "pdf", "anexo", "assinatura", "documento")) score += 6;
        if (request.sigilo()) score += 6;
        if (request.promptInjectionDetected()) score -= 8;
        if (request.quarantinedContext()) score -= 10;
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
