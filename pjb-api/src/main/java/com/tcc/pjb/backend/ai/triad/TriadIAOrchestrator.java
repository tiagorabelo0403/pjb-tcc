package com.tcc.pjb.backend.ai.triad;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.contract.DomainAgent;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.enums.IADomain;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;
import com.tcc.pjb.backend.ai.core.model.CognitiveOutput;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class TriadIAOrchestrator {

    private final List<DomainAgent> domainAgents;

    
    public IAResponse executar(IARequest request) {
        CognitiveContext ctx = new CognitiveContext(request);
        CognitiveOutput out = run(ctx);

        return IAResponse.builder()
                .origem("TRIAD")
                .texto(out.content())
                .status(IAResponse.StatusIA.INDETERMINADO)
                .confianca(0.0)
                .dataGeracao(Instant.now())
                .alertasCriticos(Collections.emptyList())
                .metadados(Collections.emptyMap())
                .essence(Collections.emptyMap())
                .evidencias(Collections.emptyList())
                .build();
    }

    
    public CognitiveOutput run(CognitiveContext ctx) {
        if (ctx == null || ctx.request() == null) {
            return new CognitiveOutput("GERAL", "INDEFINIDO", "Requisição inválida.");
        }

        String domainStr = ctx.dominioPrimario();
        String roleStr = ctx.papelSolicitante();
        IADomain requested = IADomain.fromString(domainStr);

        if (domainAgents != null) {
            domainAgents.stream()
                    .filter(a -> a != null && a.domain() == requested)
                    .findFirst()
                    .ifPresent(a -> a.support(ctx));
        }

        
        String base = "TriadIA (compat): domínio=" + domainStr + ", papel=" + roleStr + ". ";
        String detail = ctx.alertas().isEmpty()
                ? "Sem enriquecimento por agente (nenhum agente registrado para o domínio)."
                : ("Alertas: " + String.join(" | ", ctx.alertas()));

        return new CognitiveOutput(domainStr, roleStr, base + detail);
    }
}
