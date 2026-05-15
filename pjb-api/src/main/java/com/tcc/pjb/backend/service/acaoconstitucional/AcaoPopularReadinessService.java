package com.tcc.pjb.backend.service.acaoconstitucional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AcaoPopularReadinessService {

    private static final int PRAZO_PRESCRICIONAL_ANOS = 5;

    public enum ObjetoAcaoPopular {
        PATRIMONIO_PUBLICO,
        MORALIDADE_ADMINISTRATIVA,
        MEIO_AMBIENTE,
        PATRIMONIO_HISTORICO_CULTURAL
    }

    public record AcaoPopularInput(
            String processoId,
            boolean autoreECidadaoBrasileiro,
            boolean possuiTituloEleitor,
            boolean atoLesivoConfigurado,
            ObjetoAcaoPopular objeto,
            LocalDate dataAto
    ) {}

    public record AcaoPopularResult(
            boolean cabivel,
            boolean dentroDoPrazo,
            int anosRestantes,
            List<String> requisitosAtendidos,
            List<String> impeditivos
    ) {}

    public AcaoPopularResult avaliar(AcaoPopularInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (!input.autoreECidadaoBrasileiro()) {
            impeditivos.add("Legitimidade ativa restrita a cidadão brasileiro (CF art. 5º LXXIII, Lei 4.717/65 art. 1º).");
        } else {
            atendidos.add("Autor é cidadão brasileiro.");
        }
        if (!input.possuiTituloEleitor()) {
            impeditivos.add("Cidadania deve ser comprovada pelo título de eleitor (Lei 4.717/65 art. 1º §3º).");
        } else {
            atendidos.add("Título de eleitor comprovado.");
        }
        if (!input.atoLesivoConfigurado()) {
            impeditivos.add("Ato lesivo ao patrimônio público não demonstrado (Lei 4.717/65 art. 2º).");
        } else {
            atendidos.add("Ato lesivo ao " + descricaoObjeto(input.objeto()) + " configurado.");
        }

        long anosDecorridos = input.dataAto() != null
                ? ChronoUnit.YEARS.between(input.dataAto(), LocalDate.now()) : 0;
        boolean dentroDoPrazo = anosDecorridos < PRAZO_PRESCRICIONAL_ANOS;
        int anosRestantes = (int) Math.max(0, PRAZO_PRESCRICIONAL_ANOS - anosDecorridos);

        if (!dentroDoPrazo) {
            impeditivos.add(String.format(
                    "Ação popular prescrita: prazo de 5 anos esgotado (%d anos desde o ato — Lei 4.717/65 art. 21).",
                    anosDecorridos));
        }

        return new AcaoPopularResult(impeditivos.isEmpty(), dentroDoPrazo, anosRestantes, atendidos, impeditivos);
    }

    private String descricaoObjeto(ObjetoAcaoPopular obj) {
        if (obj == null) return "patrimônio público";
        return switch (obj) {
            case PATRIMONIO_PUBLICO -> "patrimônio público";
            case MORALIDADE_ADMINISTRATIVA -> "moralidade administrativa";
            case MEIO_AMBIENTE -> "meio ambiente";
            case PATRIMONIO_HISTORICO_CULTURAL -> "patrimônio histórico e cultural";
        };
    }
}
