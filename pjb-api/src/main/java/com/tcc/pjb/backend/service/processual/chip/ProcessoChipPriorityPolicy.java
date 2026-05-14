package com.tcc.pjb.backend.service.processual.chip;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class ProcessoChipPriorityPolicy {

    private ProcessoChipPriorityPolicy() {}

    public record ProcessoComChips(
            String processoId,
            Collection<ProcessoChipTipo> chips
    ) {}

    public List<ProcessoComChips> ordenar(List<ProcessoComChips> processos) {
        return processos.stream()
                .sorted(Comparator.comparingInt(
                        (ProcessoComChips p) -> pontuacao(p.chips())).reversed())
                .toList();
    }

    private int pontuacao(Collection<ProcessoChipTipo> chips) {
        int score = 0;
        for (ProcessoChipTipo chip : chips) {
            score += switch (chip) {
                case REU_PRESO, TUTELA_URGENCIA -> 100;
                case SAUDE -> 90;
                case ALIMENTOS, MENOR_ENVOLVIDO -> 80;
                case IDOSO, PESSOA_COM_DEFICIENCIA -> 60;
                case PRAZO_VENCIDO -> 50;
                case MANDADO_DEVOLVIDO, EXECUCAO_PARADA -> 40;
                case PERICIA_PENDENTE -> 30;
                case ACORDO_PROVAVEL -> 20;
                default -> 10;
            };
        }
        return score;
    }
}
