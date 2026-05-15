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
            boolean extinta,
            Optional<ExtincaoPunibilidadeCausa> causa,
            List<String> analise
    ) {}

    public ExtincaoResult verificar(ExtincaoInput input) {
        List<String> analise = new ArrayList<>();

        if (input.morteConfirmada()) {
            return extinta(new ExtincaoPunibilidadeCausa.MorteDoAgente("certidão apresentada"), analise);
        }
        if (input.abolitioCriminis()) {
            return extinta(new ExtincaoPunibilidadeCausa.AbolitioCriminis("lei nova"), analise);
        }
        if (input.anistiaLegal()) {
            return extinta(new ExtincaoPunibilidadeCausa.Anistia("lei aplicável"), analise);
        }
        if (input.gracaOuIndulto()) {
            return extinta(new ExtincaoPunibilidadeCausa.Indulto("decreto aplicável"), analise);
        }

        if (input.dataFato() != null && input.dataCondenacao() == null) {
            int prazo = prazoPrescricao(input.penaMaximaAbstrataAnos());
            LocalDate limite = input.dataFato().plusYears(prazo);
            if (LocalDate.now().isAfter(limite)) {
                analise.add(String.format("Prescrição da pretensão punitiva: prazo %d anos, vencido em %s.", prazo, limite));
                return extinta(new ExtincaoPunibilidadeCausa.Prescricao("pretensão punitiva", prazo), analise);
            }
            analise.add(String.format("PPP: prazo %d anos, vence em %s.", prazo, limite));
        }

        if (input.dataTransitoJulgado() != null && input.penaCondenadaAnos() > 0) {
            int prazo = prazoPrescricao(input.penaCondenadaAnos());
            LocalDate limite = input.dataTransitoJulgado().plusYears(prazo);
            if (LocalDate.now().isAfter(limite)) {
                analise.add(String.format("Prescrição executória: prazo %d anos, vencido em %s.", prazo, limite));
                return extinta(new ExtincaoPunibilidadeCausa.Prescricao("executória", prazo), analise);
            }
            analise.add(String.format("PPE: prazo %d anos, vence em %s.", prazo, limite));
        }

        if (input.acaoPrivada() && input.dataFato() != null && input.dataRecebimentoDenuncia() == null) {
            LocalDate limiteDecadencia = input.dataFato().plusDays(180);
            if (LocalDate.now().isAfter(limiteDecadencia)) {
                int dias = (int) Period.between(input.dataFato(), LocalDate.now()).toTotalMonths() * 30;
                return extinta(new ExtincaoPunibilidadeCausa.Decadencia(dias), analise);
            }
        }

        analise.add("Nenhuma causa extintiva identificada no momento.");
        return new ExtincaoResult(false, Optional.empty(), analise);
    }

    private ExtincaoResult extinta(ExtincaoPunibilidadeCausa causa, List<String> analise) {
        analise.add("EXTINÇÃO DA PUNIBILIDADE: " + causa.fundamentacao());
        return new ExtincaoResult(true, Optional.of(causa), analise);
    }

    private int prazoPrescricao(int penaAnos) {
        for (int i = 0; i < MAXIMOS_PENA_ANOS.length; i++) {
            if (penaAnos <= MAXIMOS_PENA_ANOS[i]) return PRAZOS_PRESCRICAO_ANOS[i];
        }
        return 20;
    }
}
