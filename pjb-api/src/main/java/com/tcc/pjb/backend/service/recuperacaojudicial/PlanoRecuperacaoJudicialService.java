package com.tcc.pjb.backend.service.recuperacaojudicial;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanoRecuperacaoJudicialService {

    public enum ResultadoVotacaoAG {
        MAIORIA_DUPLA_ATINGIDA,
        CRAM_DOWN_CONFIGURADO,
        MAIORIA_NAO_ATINGIDA,
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
            ResultadoVotacaoAG apuracao,
            List<String> classesComMaioria,
            List<String> classesSemMaioria,
            List<String> analise,
            String sinalizacao
    ) {}

    public PlanoVotacaoResult avaliarVotacao(PlanoVotacaoInput input) {
        List<String> comMaioria = new ArrayList<>();
        List<String> semMaioria = new ArrayList<>();
        List<String> analise = new ArrayList<>();

        for (VotoClasse vc : input.classes()) {
            if (vc.aprovada()) {
                comMaioria.add(vc.nomeclasse());
                analise.add(String.format("Classe '%s': maioria dupla atingida (%d/%d credores, %d/%d valor).",
                        vc.nomeclasse(), vc.credoresFavoraveis(), vc.credoresTotal(),
                        vc.valorCreditosFavoraveis(), vc.valorTotalCreditos()));
            } else {
                semMaioria.add(vc.nomeclasse());
                analise.add(String.format("Classe '%s': maioria dupla não atingida (%d/%d credores, %d/%d valor).",
                        vc.nomeclasse(), vc.credoresFavoraveis(), vc.credoresTotal(),
                        vc.valorCreditosFavoraveis(), vc.valorTotalCreditos()));
            }
        }

        int totalClasses = input.classes().size();

        if (semMaioria.isEmpty()) {
            return new PlanoVotacaoResult(ResultadoVotacaoAG.MAIORIA_DUPLA_ATINGIDA,
                    List.copyOf(comMaioria), List.copyOf(semMaioria), List.copyOf(analise),
                    "Maioria dupla atingida em todas as classes — apuração sujeita à homologação judicial (art. 58).");
        }

        boolean cramDownConfiguravel = !comMaioria.isEmpty()
                && (double) comMaioria.size() / totalClasses > 0.5;

        if (cramDownConfiguravel) {
            analise.add("Possível requisito a conferir: cram down — mais de 50% das classes com maioria dupla (art. 58 §1º) — sujeito à análise judicial de abusividade.");
            return new PlanoVotacaoResult(ResultadoVotacaoAG.CRAM_DOWN_CONFIGURADO,
                    List.copyOf(comMaioria), List.copyOf(semMaioria), List.copyOf(analise),
                    "Cram down possivelmente configurado — conferir requisitos do art. 58 §1º com o magistrado.");
        }

        return new PlanoVotacaoResult(ResultadoVotacaoAG.MAIORIA_NAO_ATINGIDA,
                List.copyOf(comMaioria), List.copyOf(semMaioria), List.copyOf(analise),
                "Maioria dupla não atingida nas classes necessárias — checklist sujeito à validação judicial.");
    }
}
