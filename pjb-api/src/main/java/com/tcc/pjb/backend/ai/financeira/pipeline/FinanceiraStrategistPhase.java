package com.tcc.pjb.backend.ai.financeira.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.common.AiSafetyGuard;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhase;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;

@Component
public class FinanceiraStrategistPhase implements CognitivePhase {

    private final AiSafetyGuard safety;

    public FinanceiraStrategistPhase(AiSafetyGuard safety) {
        this.safety = safety;
    }

    @Override
    public CognitivePhaseName name() {
        return CognitivePhaseName.THINK;
    }

    @Override
    public void execute(AgentExecutionContext ctx) {
        IARequest req = ctx.request();
        String capability = CapabilityStrings.canonical(ctx.capability());

        String input = bestEffortInput(req);
        AiSafetyGuard.GuardResult guard = safety.avaliarEntrada(input);
        if (guard.bloqueado()) {
            ctx.failFast("blocked_by_safety_guard");
            ctx.setDraft("Entrada bloqueada por política de segurança. Refaça o pedido sem dados sensíveis ou instruções indevidas.");
            ctx.putPlan("blocked", true);
            ctx.putPlan("blockReasons", guard.bloqueios());
            return;
        }

        List<String> keywords = extractKeywords(input, 10);
        List<String> queries = new ArrayList<>();

        queries.add(buildQuery(capability, keywords, "custas"));
        if (ctx.version().isAtLeast(com.tcc.pjb.backend.platform.versioning.ApiVersion.V2)) {
            queries.add(buildQuery(capability, keywords, "provisao"));
        }
        if (ctx.version().isAtLeast(com.tcc.pjb.backend.platform.versioning.ApiVersion.V3)) {
            queries.add(buildQuery(capability, keywords, "auditoria"));
        }

        ctx.putPlan("capability", capability);
        ctx.putPlan("version", ctx.version().name());
        ctx.putPlan("queries", List.copyOf(queries));
        ctx.putPlan("topK", ctx.version().isAtLeast(com.tcc.pjb.backend.platform.versioning.ApiVersion.V3) ? 12 : 8);
        ctx.putPlan("filters", Map.of("domain", "finance"));
        if (!guard.alertas().isEmpty()) {
            ctx.putPlan("safetyAlerts", guard.alertas());
        }
    }

    private static String bestEffortInput(IARequest req) {
        if (req == null) return "";
        String[] keys = {"texto", "mensagem", "descricao", "input", "prompt", "titulo"};
        String best = "";
        for (String k : keys) {
            String v = req.getSafeString(k);
            if (v != null && v.length() > best.length()) best = v;
        }
        if (best.isBlank()) best = String.valueOf(req.getAcao());
        return best;
    }

    private static List<String> extractKeywords(String text, int max) {
        if (text == null || text.isBlank()) return List.of();
        String norm = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (norm.isBlank()) return List.of();
        String[] parts = norm.split(" ");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p.length() < 3) continue;
            if (out.contains(p)) continue;
            out.add(p);
            if (out.size() >= max) break;
        }
        return List.copyOf(out);
    }

    private static String buildQuery(String capability, List<String> keywords, String flavor) {
        String cap = (capability == null || capability.isBlank()) ? "FINANCE" : capability;
        String k = (keywords == null || keywords.isEmpty()) ? "" : String.join(" ", keywords);
        return (cap + " " + flavor + " " + k).trim();
    }
}
