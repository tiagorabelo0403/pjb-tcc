package com.tcc.pjb.backend.service.processo.autuacao;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RetificacaoAutuacaoApplicator {

    public record AplicacaoInput(
            UUID processoId,
            UUID retificacaoId,
            RetificacaoAutuacaoStatus statusAtual,
            String operadorId,
            boolean aprovacaoJudicialConfirmada
    ) {}

    public record AplicacaoResultado(
            UUID processoId,
            RetificacaoAutuacaoStatus statusNovo,
            boolean aplicada,
            String mensagem
    ) {}

    private final RetificacaoAutuacaoPolicy policy;

    public RetificacaoAutuacaoApplicator(RetificacaoAutuacaoPolicy policy) {
        this.policy = policy;
    }

    public AplicacaoResultado aplicar(AplicacaoInput input) {
        if (input.statusAtual().eTerminal()) {
            return new AplicacaoResultado(input.processoId(), input.statusAtual(),
                    false, "Retificação em status terminal — não pode ser aplicada novamente.");
        }
        if (input.statusAtual() != RetificacaoAutuacaoStatus.APROVADA) {
            return new AplicacaoResultado(input.processoId(), input.statusAtual(),
                    false, "Retificação deve estar APROVADA antes de ser aplicada.");
        }
        if (!input.aprovacaoJudicialConfirmada()) {
            return new AplicacaoResultado(input.processoId(), RetificacaoAutuacaoStatus.AGUARDANDO_CONFERENCIA,
                    false, "Retificação exige aprovação judicial confirmada.");
        }
        return new AplicacaoResultado(input.processoId(), RetificacaoAutuacaoStatus.APLICADA,
                true, "Retificação aplicada com sucesso por " + input.operadorId() + ".");
    }
}
