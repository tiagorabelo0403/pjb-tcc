package com.tcc.pjb.backend.service.violenciadomestica;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ViolenciaDomesticaChecklistService {

    public enum TipoViolencia {
        FISICA, PSICOLOGICA, SEXUAL, PATRIMONIAL, MORAL
    }

    public enum TipoMedidaProtetiva {
        PROIBICAO_CONTATO,
        AFASTAMENTO_LAR,
        PROIBICAO_APROXIMACAO,
        SUSPENSAO_POSSE_ARMA,
        PRESTACAO_ALIMENTOS_PROVISORIOS,
        RESTRICAO_VISITAS_FILHOS
    }

    public enum SituacaoRisco { ALTO, MEDIO, BAIXO }

    public record ViolenciaDomesticaInput(
            List<TipoViolencia> tipos,
            boolean flagranteAtual,
            boolean reincidencia,
            boolean filhosEnvolvidos,
            boolean agressorComArma,
            boolean relacionamentoAtivo
    ) {}

    public record MedidaProtetiva(
            TipoMedidaProtetiva tipo,
            int prazoApreciacaoHoras,
            String fundamentoLegal
    ) {}

    public record ViolenciaDomesticaResult(
            SituacaoRisco risco,
            List<MedidaProtetiva> medidasRecomendadas,
            boolean audienciaCustodiaObrigatoria,
            boolean jecrimInaplicavel,
            String qualificadoraPenal,
            String observacao
    ) {}

    public ViolenciaDomesticaResult avaliar(ViolenciaDomesticaInput input) {
        List<MedidaProtetiva> medidas = new ArrayList<>();

        medidas.add(new MedidaProtetiva(
                TipoMedidaProtetiva.PROIBICAO_CONTATO, 48,
                "Lei 11.340/06 art. 22, III, a"));

        if (input.tipos().contains(TipoViolencia.FISICA) || input.flagranteAtual()) {
            medidas.add(new MedidaProtetiva(
                    TipoMedidaProtetiva.AFASTAMENTO_LAR, 48,
                    "Lei 11.340/06 art. 22, II"));
            medidas.add(new MedidaProtetiva(
                    TipoMedidaProtetiva.PROIBICAO_APROXIMACAO, 48,
                    "Lei 11.340/06 art. 22, III, b"));
        }

        if (input.agressorComArma()) {
            medidas.add(new MedidaProtetiva(
                    TipoMedidaProtetiva.SUSPENSAO_POSSE_ARMA, 48,
                    "Lei 11.340/06 art. 22, I"));
        }

        if (input.tipos().contains(TipoViolencia.PATRIMONIAL)) {
            medidas.add(new MedidaProtetiva(
                    TipoMedidaProtetiva.PRESTACAO_ALIMENTOS_PROVISORIOS, 48,
                    "Lei 11.340/06 art. 22, V"));
        }

        if (input.filhosEnvolvidos()) {
            medidas.add(new MedidaProtetiva(
                    TipoMedidaProtetiva.RESTRICAO_VISITAS_FILHOS, 48,
                    "Lei 11.340/06 art. 22, IV"));
        }

        String qualificadora = input.tipos().contains(TipoViolencia.FISICA)
                ? "CP art. 129 §9 — lesão corporal dolosa em contexto doméstico (pena: 3 meses a 3 anos)"
                : "";

        StringBuilder obs = new StringBuilder(
                "JECRIM inaplicável (Lei 11.340/06 art. 41). Prazo de apreciação das medidas: 48h (art. 18).");
        if (input.reincidencia()) {
            obs.append(" Reincidência: ponderar decretação de prisão preventiva (art. 20).");
        }

        return new ViolenciaDomesticaResult(
                calcularRisco(input),
                List.copyOf(medidas),
                input.flagranteAtual(),
                true,
                qualificadora,
                obs.toString()
        );
    }

    private SituacaoRisco calcularRisco(ViolenciaDomesticaInput input) {
        int score = 0;
        if (input.tipos().contains(TipoViolencia.SEXUAL))     score += 4;
        if (input.tipos().contains(TipoViolencia.FISICA))     score += 3;
        if (input.agressorComArma())                          score += 3;
        if (input.flagranteAtual())                           score += 2;
        if (input.reincidencia())                             score += 2;
        if (input.filhosEnvolvidos())                         score += 1;
        if (input.relacionamentoAtivo())                      score += 1;
        if (input.tipos().contains(TipoViolencia.PSICOLOGICA)) score += 1;
        if (input.tipos().contains(TipoViolencia.PATRIMONIAL)) score += 1;
        if (score >= 4) return SituacaoRisco.ALTO;
        if (score >= 2) return SituacaoRisco.MEDIO;
        return SituacaoRisco.BAIXO;
    }
}
