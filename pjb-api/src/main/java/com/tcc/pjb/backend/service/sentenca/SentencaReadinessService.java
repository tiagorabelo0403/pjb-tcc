package com.tcc.pjb.backend.service.sentenca;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SentencaReadinessService {

    public record SentencaReadinessInput(
            UUID processoId,
            SentencaTipo tipoEsperado,
            boolean instrucaoEncerrada,
            boolean alegacoesFinaisJuntadas,
            boolean peritoLaudoJuntado,
            boolean periciaNecessaria,
            boolean todasPendenciasResolvidas,
            boolean minutaElaborada,
            boolean revisaoGabinete,
            boolean publicacaoAgendada
    ) {}

    public record SentencaReadinessResult(
            boolean apta,
            List<String> pendencias,
            String fundamentacao
    ) {}

    public SentencaReadinessResult avaliar(SentencaReadinessInput input) {
        List<String> pendencias = new ArrayList<>();

        if (!input.instrucaoEncerrada()) {
            pendencias.add("Instrução processual não encerrada.");
        }
        if (!input.alegacoesFinaisJuntadas()) {
            pendencias.add("Alegações finais pendentes de apresentação.");
        }
        if (input.periciaNecessaria() && !input.peritoLaudoJuntado()) {
            pendencias.add("Laudo pericial necessário e não juntado aos autos.");
        }
        if (!input.todasPendenciasResolvidas()) {
            pendencias.add("Existem questões incidentais pendentes de resolução.");
        }
        if (!input.minutaElaborada()) {
            pendencias.add("Minuta da sentença não elaborada.");
        }
        if (!input.revisaoGabinete()) {
            pendencias.add("Sentença não revisada pelo gabinete.");
        }
        if (!input.publicacaoAgendada()) {
            pendencias.add("Publicação da sentença não agendada.");
        }

        boolean apta = pendencias.isEmpty();
        return new SentencaReadinessResult(apta, List.copyOf(pendencias),
                apta ? input.tipoEsperado() + " — apta para prolação."
                     : "Sentença com " + pendencias.size() + " pendência(s).");
    }
}
