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
            boolean aplicavel,
            int mesesMinimos,
            int mesesFaltantes,
            List<String> pendenciasIdentificadas,
            List<String> condicoesObrigatorias,
            String sinalizacao
    ) {}

    private static final int PENA_MINIMA_MESES = 24;
    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";
    private static final String SINAL_NAO_APLICAVEL =
            "Instituto não aplicável ao caso — conferir fundamento legal com o magistrado.";

    public LivramentoResult avaliar(LivramentoInput input) {
        List<String> pendencias = new ArrayList<>();
        List<String> condicoes = List.of(
                "Obter ocupação lícita (CP art. 85 I)",
                "Comunicar à autoridade qualquer mudança de endereço (CP art. 85 II)",
                "Recolher-se à habitação dentro do horário designado (CP art. 85 III)",
                "Não mudar de Comarca sem aviso prévio ao Juiz (CP art. 85 IV)");

        if (input.penaTotalMeses() < PENA_MINIMA_MESES) {
            pendencias.add("Possível requisito a conferir: pena inferior a 2 anos — livramento condicional pode ser inaplicável (CP art. 83).");
            return new LivramentoResult(false, 0, 0, List.copyOf(pendencias), List.of(), SINAL_NAO_APLICAVEL);
        }
        if (input.habitualidadeCriminal()) {
            pendencias.add("Pendência identificada: habitualidade criminal — conferir vedação (CP art. 83 §único I).");
        }
        if (input.associacaoCriminosa()) {
            pendencias.add("Pendência identificada: associação criminosa — conferir vedação (CP art. 83 §único II).");
        }

        int mesesMinimos = calcularFracao(input);

        if (!input.primarioBoaConduta()) {
            pendencias.add("Pendência identificada: comportamento insatisfatório durante a execução (CP art. 83 III) — sujeito à avaliação do juízo.");
        }
        if (!input.danoReparadoOuImpossivel()) {
            pendencias.add("Pendência identificada: dano não reparado sem comprovação de impossibilidade (CP art. 83 IV).");
        }
        if (input.mesesCumpridos() < mesesMinimos) {
            pendencias.add(String.format(
                    "Pendência identificada: fração mínima não cumprida — %d meses cumpridos de %d necessários.",
                    input.mesesCumpridos(), mesesMinimos));
        }

        int faltam = Math.max(0, mesesMinimos - input.mesesCumpridos());
        return new LivramentoResult(true, mesesMinimos, faltam, List.copyOf(pendencias), condicoes,
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
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
