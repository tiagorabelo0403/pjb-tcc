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
            boolean homologacaoViavel,
            boolean vinculaTodaAClasse,
            double percentualAcordo,
            List<String> analise,
            List<String> impeditivos
    ) {}

    private static final double PERCENTUAL_MINIMO_HOMOLOGACAO = 0.60;

    public RecuperacaoExtrajudicialResult avaliar(RecuperacaoExtrajudicialInput input) {
        List<String> analise = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        double percentual = input.credoresClasseTotal() > 0
                ? (double) input.credoresAcordo() / input.credoresClasseTotal() : 0;

        analise.add(String.format(
                "Credores da classe com acordo: %d/%d (%.1f%%).",
                input.credoresAcordo(), input.credoresClasseTotal(), percentual * 100));

        if (!input.excluiCredoresTrabalhistasFiscaisPrevisionalAcidentarios()) {
            impeditivos.add("CRJ não pode incluir créditos trabalhistas, tributários, previdenciários e acidentários (Lei 11.101/2005 art. 161 §1º).");
        }

        if (percentual < PERCENTUAL_MINIMO_HOMOLOGACAO) {
            impeditivos.add(String.format(
                    "Menos de 60%% dos credores da classe firmaram acordo (%.1f%%) — homologação não compulsória (art. 161 §2º).",
                    percentual * 100));
        } else {
            analise.add(String.format("Acordo atingiu %.1f%% — homologação vincula toda a classe (art. 163).", percentual * 100));
        }

        boolean vincula = percentual >= PERCENTUAL_MINIMO_HOMOLOGACAO;

        return new RecuperacaoExtrajudicialResult(
                impeditivos.isEmpty(), vincula, percentual * 100, analise, impeditivos);
    }
}
