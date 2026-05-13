package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewCoreFieldRequirementResolver {

    private final NationalProceduralReviewMessages messages;

    public NationalProceduralReviewCoreFieldRequirementResolver(NationalProceduralReviewMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralReviewInputSlice assess(Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        if (NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("classe")))) {
            missingInputs.add("classeProcessual");
            blockingIssues.add(messages.missingClasseBlocking());
        }
        if (NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("assunto")))
                && NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("objetoProcessual")))) {
            missingInputs.add("assuntoOuObjetoProcessual");
            blockingIssues.add(messages.missingObjetoBlocking());
        }
        if (NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("pedidoPrincipal")))
                && NationalProceduralRoutingSupport.isBlank(NationalProceduralRoutingSupport.text(safePayload.get("pedidos")))) {
            missingInputs.add("pedidoPrincipal");
            blockingIssues.add(messages.missingPedidoBlocking());
        }
        return new NationalProceduralReviewInputSlice(List.copyOf(missingInputs), List.copyOf(blockingIssues));
    }
}
