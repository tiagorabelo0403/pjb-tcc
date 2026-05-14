package com.tcc.pjb.backend.service.sentenca;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExequibilidadeDecisaoService {

    public record ExequibilidadeInput(
            UUID processoId,
            SentencaTipo tipo,
            boolean transitadoEmJulgado,
            boolean liquidacaoNecessaria,
            boolean liquidacaoConcluida,
            boolean tituloExecutivoFormado,
            boolean devedorCitado,
            boolean prazoVoluntarioCumprido,
            boolean cumprimentoSentencaIniciado
    ) {}

    public record ExequibilidadeResult(
            boolean exequivel,
            boolean exigeLiquidacao,
            List<String> impedimentos,
            String fundamentacao
    ) {}

    public ExequibilidadeResult avaliar(ExequibilidadeInput input) {
        List<String> impedimentos = new ArrayList<>();

        if (!input.tipo().transitaEmJulgado() && !input.tituloExecutivoFormado()) {
            return new ExequibilidadeResult(false, false,
                    List.of("Decisão não forma título executivo judicial."),
                    "Decisão sem eficácia executiva.");
        }
        if (!input.transitadoEmJulgado() && !input.cumprimentoSentencaIniciado()) {
            impedimentos.add("Trânsito em julgado não verificado — cumprimento provisório possível (art. 520, CPC).");
        }
        if (input.liquidacaoNecessaria() && !input.liquidacaoConcluida()) {
            impedimentos.add("Liquidação de sentença necessária e não concluída (art. 509, CPC).");
        }
        if (!input.devedorCitado()) {
            impedimentos.add("Devedor não citado no cumprimento de sentença (art. 523, CPC).");
        }

        boolean exequivel = impedimentos.stream()
                .noneMatch(i -> i.contains("Liquidação de sentença necessária"));

        return new ExequibilidadeResult(exequivel, input.liquidacaoNecessaria(),
                List.copyOf(impedimentos),
                exequivel ? "Decisão exequível — cumprimento de sentença viável."
                          : "Exequibilidade suspensa por liquidação pendente.");
    }
}
