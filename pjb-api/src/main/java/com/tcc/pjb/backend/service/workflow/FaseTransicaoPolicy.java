package com.tcc.pjb.backend.service.workflow;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class FaseTransicaoPolicy {

    private FaseTransicaoPolicy() {}

    public record TransicaoInput(
            RitoProcessual rito,
            FaseProcessual faseAtual,
            FaseProcessual faseAlvo,
            List<String> atosConcluidosNaFase
    ) {}

    public record TransicaoResult(
            boolean permitida,
            List<String> bloqueios,
            List<String> alertas
    ) {}

    public static TransicaoResult avaliar(TransicaoInput input) {
        Set<FaseProcessual> destinos = transicoesPermitidas(input.rito(), input.faseAtual());
        if (!destinos.contains(input.faseAlvo())) {
            return new TransicaoResult(false,
                    List.of("Transição de " + input.faseAtual() + " para "
                            + input.faseAlvo() + " não é válida para o rito "
                            + input.rito() + "."),
                    List.of());
        }
        List<String> bloqueios = atosObrigatoriosFaltantes(input);
        return new TransicaoResult(bloqueios.isEmpty(), bloqueios, List.of());
    }

    private static Set<FaseProcessual> transicoesPermitidas(RitoProcessual rito,
            FaseProcessual fase) {
        return switch (fase) {
            case AUTUACAO -> EnumSet.of(FaseProcessual.DISTRIBUICAO);
            case DISTRIBUICAO -> EnumSet.of(FaseProcessual.CITACAO);
            case CITACAO -> EnumSet.of(FaseProcessual.RESPOSTA, FaseProcessual.SANEAMENTO);
            case RESPOSTA -> EnumSet.of(FaseProcessual.SANEAMENTO);
            case SANEAMENTO -> EnumSet.of(FaseProcessual.JULGAMENTO);
            case JULGAMENTO -> EnumSet.of(FaseProcessual.ARQUIVAMENTO);
            default -> EnumSet.noneOf(FaseProcessual.class);
        };
    }

    private static List<String> atosObrigatoriosFaltantes(TransicaoInput input) {
        return switch (input.faseAtual()) {
            case CITACAO -> input.atosConcluidosNaFase().contains("MANDADO_CUMPRIDO")
                    || input.atosConcluidosNaFase().contains("CITACAO_POSTAL_CONFIRMADA")
                    ? List.of()
                    : List.of("Citação ainda não realizada ou confirmada.");
            case SANEAMENTO -> input.atosConcluidosNaFase().contains("SANEAMENTO_PUBLICADO")
                    ? List.of()
                    : List.of("Despacho de saneamento não publicado.");
            default -> List.of();
        };
    }
}
