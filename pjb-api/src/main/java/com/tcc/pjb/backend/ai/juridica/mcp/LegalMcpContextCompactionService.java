package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpContextCompactionPlan;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpContextCompactionService {

    public LegalMcpContextCompactionPlan resolve(LegalMcpServerProfile.ResolveRequest request,
                                                 LegalEvalReplayResult evaluation) {
        List<String> reasons = new ArrayList<>();
        int historyDepth = request.history() == null ? 0 : request.history().size();
        int retained = historyDepth <= 4 ? historyDepth : request.promptInjectionDetected() || request.quarantinedContext() ? 2 : request.sigilo() ? 3 : 4;
        String policy;
        String status;
        if (historyDepth >= 6 || (evaluation != null && evaluation.adaptationHints() != null && "SLIDING_COMPACTION".equals(evaluation.adaptationHints().get("contextCompactionPolicy")))) {
            policy = "SLIDING_COMPACTION";
            status = "ACTIVE";
            reasons.add("Histórico profundo pede compactação deslizante para reduzir ruído e preservar fatos jurídicos centrais.");
        } else if (historyDepth > 0) {
            policy = "FULL_CONTEXT_ALLOWED";
            status = "BYPASSED";
            reasons.add("Histórico curto permanece íntegro sem compactação adicional.");
        } else {
            policy = "EMPTY_HISTORY_FAST_PATH";
            status = "BYPASSED";
            reasons.add("Sem histórico relevante para compactar neste turno.");
        }
        if (request.promptInjectionDetected() || request.quarantinedContext()) {
            reasons.add("Contexto sensível exige retenção mínima e externalização segura do restante.");
            retained = Math.min(retained, 2);
            status = "STRICT_ACTIVE";
            policy = "SLIDING_COMPACTION";
        }
        return new LegalMcpContextCompactionPlan(
                status,
                policy,
                retained,
                retained < historyDepth ? "TRACE_AND_MEMORY_EXTERNALIZATION" : "INLINE_ONLY",
                List.copyOf(reasons)
        );
    }
}
