package com.tcc.pjb.backend.service.execucaopenal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProgressaoRegimeService {

    public record ProgressaoInput(
            String processoId,
            int penaTotalMeses,
            int mesesCumpridos,
            RegimePrisionalTipo regimeAtual,
            boolean hediondo,
            boolean reincidenteEspecifico,
            boolean resultadoMorte,
            boolean reincidente,
            boolean comViolenciaOuGraveAmeaca,
            boolean comportamentoSatisfatorio,
            LocalDate dataInicioCumprimento
    ) {}

    public record ProgressaoResult(
            boolean progressaoPossivel,
            RegimePrisionalTipo proximoRegime,
            int mesesNecessarios,
            int mesesFaltantes,
            LocalDate dataPrevisao,
            FracaoProgressaoRegime fracao,
            List<String> impeditivos
    ) {}

    public ProgressaoResult avaliar(ProgressaoInput input) {
        List<String> impeditivos = new ArrayList<>();

        if (input.regimeAtual() == RegimePrisionalTipo.ABERTO
                || input.regimeAtual() == RegimePrisionalTipo.ALBERGUE_DOMICILIAR) {
            return new ProgressaoResult(false, input.regimeAtual(), 0, 0, null, null,
                    List.of("Condenado já no regime mais brando aplicável."));
        }

        FracaoProgressaoRegime fracao = FracaoProgressaoRegime.resolver(
                input.hediondo(), input.reincidenteEspecifico(),
                input.resultadoMorte(), input.reincidente(),
                input.comViolenciaOuGraveAmeaca());

        int penaParaCalculo = Math.min(input.penaTotalMeses(), 40 * 12);
        int mesesNecessarios = (int) Math.ceil(penaParaCalculo * fracao.percentual() / 100.0);

        if (!input.comportamentoSatisfatorio()) {
            impeditivos.add("Comportamento insatisfatório durante a execução (LEP art. 112 §1º).");
        }
        if (input.mesesCumpridos() < mesesNecessarios) {
            impeditivos.add(String.format(
                    "Fração mínima não atingida: cumpridos %d meses, necessários %d (%d%% — %s).",
                    input.mesesCumpridos(), mesesNecessarios, fracao.percentual(), fracao.fundamentacao()));
        }

        int faltam = Math.max(0, mesesNecessarios - input.mesesCumpridos());
        LocalDate previsao = input.dataInicioCumprimento() != null
                ? input.dataInicioCumprimento().plusMonths(mesesNecessarios) : null;

        if (!impeditivos.isEmpty()) {
            return new ProgressaoResult(false, input.regimeAtual().proximo(),
                    mesesNecessarios, faltam, previsao, fracao, impeditivos);
        }

        return new ProgressaoResult(true, input.regimeAtual().proximo(),
                mesesNecessarios, 0, previsao, fracao, List.of());
    }
}
