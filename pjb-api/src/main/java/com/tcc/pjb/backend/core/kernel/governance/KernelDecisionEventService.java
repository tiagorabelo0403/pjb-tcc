package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.KernelDecisionEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.KernelDecisionEventRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KernelDecisionEventService {

    private final KernelDecisionEventRepository repository;

    @Transactional
    public KernelDecisionEvent register(Processo processo,
                                        PropostaAcordo proposta,
                                        Usuario actor,
                                        NegotiationMessageDecision decision) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(decision, "decision");
        KernelDecisionEvent event = KernelDecisionEvent.builder()
                .processoId(processo.getId())
                .propostaId(proposta != null ? proposta.getId() : null)
                .usuarioId(actor != null ? actor.getId() : null)
                .scope(decision.scope())
                .decisionCode(decision.decisionCode())
                .releaseAllowed(decision.releaseAllowed())
                .approvalRequired(decision.approvalRequired())
                .internalDraftRequired(decision.internalDraftRequired())
                .riskLevel(decision.riskLevel())
                .reasons(join(decision.reasons()))
                .mandatoryActions(join(decision.mandatoryActions()))
                .diagnostics(String.valueOf(decision.diagnostics()))
                .build();
        return repository.save(event);
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);
    }
}
