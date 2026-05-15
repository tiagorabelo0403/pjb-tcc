package com.tcc.pjb.backend.service.execucaopenal;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ExtincaoPunibilidadeService {

    private static final int[] PRAZOS_PRESCRICAO_ANOS = {2, 3, 4, 8, 12, 16, 20};
    private static final int[] MAXIMOS_PENA_ANOS = {1, 2, 4, 8, 12, 20, Integer.MAX_VALUE};

    public record ExtincaoInput(
            String processoId,
            int penaMaximaAbstrataAnos,
            int penaCondenadaAnos,
            LocalDate dataFato,
            LocalDate dataRecebimentoDenuncia,
            LocalDate dataCondenacao,
            LocalDate dataTransitoJulgado,
            boolean morteConfirmada,
            boolean anistiaLegal,
            boolean gracaOuIndulto,
            boolean abolitioCriminis,
            boolean acaoPrivada
    ) {}

    public record ExtincaoResult(
            Optional<ExtincaoPunibilidadeCausa> causaIndicada,
            List<String> indicativosIdentificados,
            String sinalizacao
    ) {}

    private static final String SINAL_INDICATIVO =
            "Indicativo de possível causa extintiva identificado — sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_SEM_INDICATIVO =
            "Nenhum indicativo de causa extintiva localizado nos dados fornecidos — conferir com o processo.";

    public ExtincaoResult verificar(ExtincaoInput input) {
        List<String> indicativos = new ArrayList<>();

        if (input.morteConfirmada()) {
            return comIndicativo(new ExtincaoPunibilidadeCausa.MorteDoAgente("certidão apresentada"), indicativos,
                    "Possível requisito a conferir: morte do agente com certidão apresentada (CP art. 107 I).");
        }
        if (input.abolitioCriminis()) {
            return comIndicativo(new ExtincaoPunibilidadeCausa.AbolitioCriminis("lei nova"), indicativos,
                    "Possível requisito a conferir: abolitio criminis — lei posterior suprime a tipicidade (CP art. 107 III).");
        }
        if (input.anistiaLegal()) {
            return comIndicativo(new ExtincaoPunibilidadeCausa.Anistia("lei aplicável"), indicativos,
                    "Possível requisito a conferir: anistia legal aplicável (CP art. 107 II).");
        }
        if (input.gracaOuIndulto()) {
            return comIndicativo(new ExtincaoPunibilidadeCausa.Indulto("decreto aplicável"), indicativos,
                    "Possível requisito a conferir: graça ou indulto — decreto aplicável (CP art. 107 II).");
        }

        if (input.dataFato() != null && input.dataCondenacao() == null) {
            int prazo = prazoPrescricao(input.penaMaximaAbstrataAnos());
            LocalDate limite = input.dataFato().plusYears(prazo);
            if (LocalDate.now().isAfter(limite)) {
                indicativos.add(String.format(
                        "Possível requisito a conferir: pretensão punitiva — prazo %d anos, limite %s ultrapassado (CP art. 109).",
                        prazo, limite));
                return comIndicativo(new ExtincaoPunibilidadeCausa.Prescricao("pretensão punitiva", prazo), indicativos, null);
            }
            indicativos.add(String.format("PPP em curso: prazo %d anos, vence em %s.", prazo, limite));
        }

        if (input.dataTransitoJulgado() != null && input.penaCondenadaAnos() > 0) {
            int prazo = prazoPrescricao(input.penaCondenadaAnos());
            LocalDate limite = input.dataTransitoJulgado().plusYears(prazo);
            if (LocalDate.now().isAfter(limite)) {
                indicativos.add(String.format(
                        "Possível requisito a conferir: prescrição executória — prazo %d anos, limite %s ultrapassado (CP art. 110).",
                        prazo, limite));
                return comIndicativo(new ExtincaoPunibilidadeCausa.Prescricao("executória", prazo), indicativos, null);
            }
            indicativos.add(String.format("PPE em curso: prazo %d anos, vence em %s.", prazo, limite));
        }

        if (input.acaoPrivada() && input.dataFato() != null && input.dataRecebimentoDenuncia() == null) {
            LocalDate limiteDecadencia = input.dataFato().plusDays(180);
            if (LocalDate.now().isAfter(limiteDecadencia)) {
                int dias = (int) Period.between(input.dataFato(), LocalDate.now()).toTotalMonths() * 30;
                indicativos.add("Possível requisito a conferir: decadência em ação privada — 180 dias ultrapassados (CPP art. 38).");
                return comIndicativo(new ExtincaoPunibilidadeCausa.Decadencia(dias), indicativos, null);
            }
        }

        indicativos.add("Nenhum indicativo de causa extintiva localizado nos dados fornecidos.");
        return new ExtincaoResult(Optional.empty(), List.copyOf(indicativos), SINAL_SEM_INDICATIVO);
    }

    private ExtincaoResult comIndicativo(ExtincaoPunibilidadeCausa causa, List<String> indicativos, String mensagemExtra) {
        if (mensagemExtra != null) indicativos.add(mensagemExtra);
        return new ExtincaoResult(Optional.of(causa), List.copyOf(indicativos), SINAL_INDICATIVO);
    }

    private int prazoPrescricao(int penaAnos) {
        for (int i = 0; i < MAXIMOS_PENA_ANOS.length; i++) {
            if (penaAnos <= MAXIMOS_PENA_ANOS[i]) return PRAZOS_PRESCRICAO_ANOS[i];
        }
        return 20;
    }
}
