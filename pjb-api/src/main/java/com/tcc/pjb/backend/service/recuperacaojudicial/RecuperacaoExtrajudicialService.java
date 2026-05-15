package com.tcc.pjb.backend.service.recuperacaojudicial;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecuperacaoExtrajudicialService {

    public record RecuperacaoExtrajudicialInput(
            String cnpj,
            int credoresClasseTotal,
            int credoresAcordo,
            boolean excluiCredoresTrabalhistasFiscaisPrevisionalAcidentarios,
            boolean planoNaoImporEmNovasMoratoriasAposProtestar
    ) {}

    public record RecuperacaoExtrajudicialResult(
            double percentualAcordo,
            List<String> indicativosIdentificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final double PERCENTUAL_MINIMO_HOMOLOGACAO = 0.60;
    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public RecuperacaoExtrajudicialResult avaliar(RecuperacaoExtrajudicialInput input) {
        List<String> indicativos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        double percentual = input.credoresClasseTotal() > 0
                ? (double) input.credoresAcordo() / input.credoresClasseTotal() : 0;

        indicativos.add(String.format(
                "Credores da classe com acordo: %d/%d (%.1f%%).",
                input.credoresAcordo(), input.credoresClasseTotal(), percentual * 100));

        if (!input.excluiCredoresTrabalhistasFiscaisPrevisionalAcidentarios()) {
            pendencias.add("Pendência identificada: plano não pode incluir créditos trabalhistas, tributários, previdenciários e acidentários (Lei 11.101/2005 art. 161 §1º).");
        }

        if (percentual < PERCENTUAL_MINIMO_HOMOLOGACAO) {
            pendencias.add(String.format(
                    "Pendência identificada: menos de 60%% dos credores firmaram acordo (%.1f%%) — homologação vinculante pode não ser cabível (art. 161 §2º).",
                    percentual * 100));
        } else {
            indicativos.add(String.format(
                    "Possível requisito a conferir: acordo atingiu %.1f%% — homologação pode vincular toda a classe (art. 163) — sujeito à análise judicial.",
                    percentual * 100));
        }

        return new RecuperacaoExtrajudicialResult(percentual * 100,
                List.copyOf(indicativos), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
