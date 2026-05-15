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
            boolean prazoNaoDecaido,
            int anosRestantes,
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public AcaoPopularResult avaliar(AcaoPopularInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (!input.autoreECidadaoBrasileiro()) {
            pendencias.add("Possível requisito a conferir: legitimidade ativa restrita a cidadão brasileiro (CF art. 5º LXXIII, Lei 4.717/65 art. 1º).");
        } else {
            verificados.add("Autor é cidadão brasileiro — conferido.");
        }
        if (!input.possuiTituloEleitor()) {
            pendencias.add("Possível requisito a conferir: título de eleitor não apresentado (Lei 4.717/65 art. 1º §3º).");
        } else {
            verificados.add("Título de eleitor comprovado — conferido.");
        }
        if (!input.atoLesivoConfigurado()) {
            pendencias.add("Possível requisito a conferir: ato lesivo ao patrimônio público não demonstrado (Lei 4.717/65 art. 2º).");
        } else {
            verificados.add("Ato lesivo ao " + descricaoObjeto(input.objeto()) + " — conferido.");
        }

        long anosDecorridos = input.dataAto() != null
                ? ChronoUnit.YEARS.between(input.dataAto(), LocalDate.now()) : 0;
        boolean prazoVigente = anosDecorridos < PRAZO_PRESCRICIONAL_ANOS;
        int anosRestantes = (int) Math.max(0, PRAZO_PRESCRICIONAL_ANOS - anosDecorridos);

        if (!prazoVigente) {
            pendencias.add(String.format(
                    "Pendência identificada: prazo prescricional de 5 anos possivelmente esgotado — %d anos desde o ato (Lei 4.717/65 art. 21).",
                    anosDecorridos));
        }

        return new AcaoPopularResult(prazoVigente, anosRestantes,
                List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
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
