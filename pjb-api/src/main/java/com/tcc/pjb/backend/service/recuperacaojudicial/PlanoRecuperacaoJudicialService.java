package com.tcc.pjb.backend.service.recuperacaojudicial;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanoRecuperacaoJudicialService {

    public enum ResultadoVotacaoAG {
        APROVADO_MAIORIA,
        APROVADO_CRAM_DOWN,
        REJEITADO,
        SEM_QUORUM
    }

    public record VotoClasse(
            String nomeclasse,
            int credoresTotal,
            int credoresFavoraveis,
            long valorTotalCreditos,
            long valorCreditosFavoraveis
    ) {
        public boolean aprovada() {
            boolean maioriaNumero = credoresTotal > 0
                    && (credoresFavoraveis * 100.0 / credoresTotal) > 50;
            boolean maioriaValor = valorTotalCreditos > 0
                    && (valorCreditosFavoraveis * 100.0 / valorTotalCreditos) > 50;
            return maioriaNumero && maioriaValor;
        }
    }

    public record PlanoVotacaoInput(
            String processoId,
            List<VotoClasse> classes,
            boolean classeTrabalhistaExcluida
    ) {}

    public record PlanoVotacaoResult(
            ResultadoVotacaoAG resultado,
            boolean cramDownAplicavel,
            List<String> classesAprovadas,
            List<String> classesRejeitadas,
            List<String> analise
    ) {}

    public PlanoVotacaoResult avaliarVotacao(PlanoVotacaoInput input) {
        List<String> aprovadas = new ArrayList<>();
        List<String> rejeitadas = new ArrayList<>();
        List<String> analise = new ArrayList<>();

        for (VotoClasse vc : input.classes()) {
            if (vc.aprovada()) {
                aprovadas.add(vc.nomeclasse());
                analise.add(String.format("Classe '%s': APROVADA (%d/%d credores, %d/%d valor).",
                        vc.nomeclasse(), vc.credoresFavoraveis(), vc.credoresTotal(),
                        vc.valorCreditosFavoraveis(), vc.valorTotalCreditos()));
            } else {
                rejeitadas.add(vc.nomeclasse());
                analise.add(String.format("Classe '%s': REJEITADA (%d/%d credores, %d/%d valor).",
                        vc.nomeclasse(), vc.credoresFavoraveis(), vc.credoresTotal(),
                        vc.valorCreditosFavoraveis(), vc.valorTotalCreditos()));
            }
        }

        int totalClasses = input.classes().size();

        if (rejeitadas.isEmpty()) {
            return new PlanoVotacaoResult(ResultadoVotacaoAG.APROVADO_MAIORIA, false,
                    aprovadas, rejeitadas, analise);
        }

        boolean cramDownPossivel = !aprovadas.isEmpty()
                && (double) aprovadas.size() / totalClasses > 0.5;

        if (cramDownPossivel) {
            analise.add("Cram down possível: mais de 50% das classes aprovaram e plano não é abusivo (art. 58 §1º).");
            return new PlanoVotacaoResult(ResultadoVotacaoAG.APROVADO_CRAM_DOWN, true,
                    aprovadas, rejeitadas, analise);
        }

        return new PlanoVotacaoResult(ResultadoVotacaoAG.REJEITADO, false,
                aprovadas, rejeitadas, analise);
    }
}
