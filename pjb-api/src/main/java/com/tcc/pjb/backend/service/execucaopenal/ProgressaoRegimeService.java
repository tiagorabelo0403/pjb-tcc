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
            RegimePrisionalTipo regimeSugerido,
            int mesesNecessarios,
            int mesesFaltantes,
            LocalDate dataPrevisaoMinima,
            FracaoProgressaoRegime fracao,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — sugestão sujeita à validação judicial.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public ProgressaoResult avaliar(ProgressaoInput input) {
        List<String> pendencias = new ArrayList<>();

        if (input.regimeAtual() == RegimePrisionalTipo.ABERTO
                || input.regimeAtual() == RegimePrisionalTipo.ALBERGUE_DOMICILIAR) {
            return new ProgressaoResult(input.regimeAtual(), 0, 0, null, null,
                    List.of("Possível requisito a conferir: condenado já no regime mais brando aplicável."),
                    SINAL_COM_PENDENCIAS);
        }

        FracaoProgressaoRegime fracao = FracaoProgressaoRegime.resolver(
                input.hediondo(), input.reincidenteEspecifico(),
                input.resultadoMorte(), input.reincidente(),
                input.comViolenciaOuGraveAmeaca());

        int penaParaCalculo = Math.min(input.penaTotalMeses(), 40 * 12);
        int mesesNecessarios = (int) Math.ceil(penaParaCalculo * fracao.percentual() / 100.0);

        if (!input.comportamentoSatisfatorio()) {
            pendencias.add("Pendência identificada: comportamento insatisfatório durante a execução (LEP art. 112 §1º) — sujeito à avaliação do juízo.");
        }
        if (input.mesesCumpridos() < mesesNecessarios) {
            pendencias.add(String.format(
                    "Pendência identificada: fração mínima não atingida — cumpridos %d meses, necessários %d (%d%% — %s).",
                    input.mesesCumpridos(), mesesNecessarios, fracao.percentual(), fracao.fundamentacao()));
        }

        int faltam = Math.max(0, mesesNecessarios - input.mesesCumpridos());
        LocalDate previsao = input.dataInicioCumprimento() != null
                ? input.dataInicioCumprimento().plusMonths(mesesNecessarios) : null;

        return new ProgressaoResult(
                input.regimeAtual().proximo(),
                mesesNecessarios, faltam, previsao, fracao,
                List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
