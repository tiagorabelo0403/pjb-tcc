package com.tcc.pjb.backend.service.execucaopenal;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LivramentoCondicionalService {

    public record LivramentoInput(
            int penaTotalMeses,
            int mesesCumpridos,
            boolean hediondo,
            boolean reincidenteEspecifico,
            boolean primarioBoaConduta,
            boolean danoReparadoOuImpossivel,
            boolean habitualidadeCriminal,
            boolean associacaoCriminosa
    ) {}

    public record LivramentoResult(
            boolean cabivel,
            boolean requisitosAtendidos,
            int mesesMinimos,
            int mesesFaltantes,
            List<String> impedimentos,
            List<String> condicoesObrigatorias
    ) {}

    private static final int PENA_MINIMA_MESES = 24;

    public LivramentoResult avaliar(LivramentoInput input) {
        List<String> impedimentos = new ArrayList<>();
        List<String> condicoes = List.of(
                "Obter ocupação lícita (CP art. 85 I)",
                "Comunicar à autoridade qualquer mudança de endereço (CP art. 85 II)",
                "Recolher-se à habitação dentro do horário designado (CP art. 85 III)",
                "Não mudar de Comarca sem aviso prévio ao Juiz (CP art. 85 IV)");

        if (input.penaTotalMeses() < PENA_MINIMA_MESES) {
            impedimentos.add("Pena inferior a 2 anos — livramento condicional incabível (CP art. 83).");
            return new LivramentoResult(false, false, 0, 0, impedimentos, List.of());
        }
        if (input.habitualidadeCriminal()) {
            impedimentos.add("Condenado por habitualidade criminal (CP art. 83 §único I).");
        }
        if (input.associacaoCriminosa()) {
            impedimentos.add("Condenado por associação criminosa (CP art. 83 §único II).");
        }

        int mesesMinimos = calcularFracao(input);

        if (!input.primarioBoaConduta()) {
            impedimentos.add("Comportamento insatisfatório durante a execução (CP art. 83 III).");
        }
        if (!input.danoReparadoOuImpossivel()) {
            impedimentos.add("Dano não reparado sem comprovação de impossibilidade (CP art. 83 IV).");
        }
        if (input.mesesCumpridos() < mesesMinimos) {
            impedimentos.add(String.format(
                    "Fração mínima não cumprida: %d meses cumpridos de %d necessários.",
                    input.mesesCumpridos(), mesesMinimos));
        }

        boolean atendidos = impedimentos.isEmpty();
        int faltam = Math.max(0, mesesMinimos - input.mesesCumpridos());
        return new LivramentoResult(true, atendidos, mesesMinimos, faltam, impedimentos, condicoes);
    }

    private int calcularFracao(LivramentoInput input) {
        if (input.hediondo() && !input.reincidenteEspecifico()) {
            return (int) Math.ceil(input.penaTotalMeses() * 2.0 / 3);
        }
        if (!input.primarioBoaConduta()) {
            return input.penaTotalMeses() / 2;
        }
        return input.penaTotalMeses() / 3;
    }
}
