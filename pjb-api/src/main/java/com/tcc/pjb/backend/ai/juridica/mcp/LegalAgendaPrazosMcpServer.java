package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalAgendaPrazosMcpServer implements LegalMcpServerProfile {

    @Override
    public LegalMcpServerDescriptor descriptor() {
        return new LegalMcpServerDescriptor(
                "MCP_AGENDA_PRAZOS",
                "MCP Agenda e Prazos",
                "AGENDA_PRAZOS",
                "STREAMABLE_HTTP_JSON_RPC_2_0",
                "SERVER_MANAGED_OAUTH2_1",
                true,
                true,
                true,
                false,
                "DEADLINE_AND_SCHEDULE_CHAIN",
                List.of("prazo_counter", "audiencia_agenda_probe", "deadline_checklist_guard"),
                List.of("calendar://procedural", "deadlines://counting", "hearings://agenda"),
                List.of("PRAZO", "AUDIENCIA", "CALENDARIO", "AGENDA"),
                List.of(
                        tool("agenda.deadline.count", "Contagem de prazo", "READ_ONLY_DEADLINE_COUNTER", false, "PRAZO", "LOW"),
                        tool("agenda.hearing.calendar", "Agenda de audiência", "READ_ONLY_HEARING_CALENDAR", true, "AGENDA", "MEDIUM"),
                        tool("agenda.deadline.checklist", "Checklist de prazo", "READ_ONLY_DEADLINE_CHECKLIST", false, "PRAZO", "LOW")
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
        int score = 12;
        
        if (capability.contains("PRAZO") || capability.contains("CONSULTA") || capability.contains("PETICAO")) score += 7;
        if (schema.contains("CHECKLIST") || schema.contains("PROCEDURAL_PLAN")) score += 6;
        if (containsHistory(request.history(), "prazo", "audiencia", "calendario", "intimacao")) score += 10;
        if (rito.contains("RECURSAL") || rito.contains("JUIZADO") || rito.contains("COMUM")) score += 3;
        if (request.sigilo()) score += 2;
        if (request.promptInjectionDetected()) score -= 2;
        if (request.quarantinedContext()) score -= 3;
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
