package com.tcc.pjb.backend.service.processo.autuacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class RetificacaoAutuacaoPolicy {

    public RetificacaoAutuacaoPolicy() {}

    public record RetificacaoInput(
            RitoProcessual ritoAtual,
            RitoProcessual ritoSolicitado,
            String classeAtual,
            String classeSolicitada,
            boolean processoJaCitado,
            boolean processoComSentenca,
            String motivoRetificacao
    ) {}

    public record RetificacaoDecisao(
            boolean permitida,
            List<String> impedimentos,
            List<String> alertas,
            String orientacao
    ) {}

    public RetificacaoDecisao avaliar(RetificacaoInput input) {
        List<String> impedimentos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        if (input.processoComSentenca()) {
            impedimentos.add("Processo com sentença transitada — retificação de autuação não permitida.");
        }
        if (input.ritoAtual() != input.ritoSolicitado()) {
            alertas.add("Alteração de rito altera prazos, competência e fluxo — exige aprovação judicial.");
            if (input.processoJaCitado()) {
                impedimentos.add("Parte já citada — alteração de rito exige nova citação se rito incompatível.");
            }
        }
        if (input.motivoRetificacao() == null || input.motivoRetificacao().isBlank()) {
            impedimentos.add("Motivo da retificação obrigatório para auditoria.");
        }

        boolean permitida = impedimentos.isEmpty();
        String orientacao = permitida
                ? "Retificação permitida — prosseguir com diff jurídico e aprovação."
                : "Retificação bloqueada: " + impedimentos.size() + " impedimento(s).";

        return new RetificacaoDecisao(permitida, impedimentos, alertas, orientacao);
    }
}
