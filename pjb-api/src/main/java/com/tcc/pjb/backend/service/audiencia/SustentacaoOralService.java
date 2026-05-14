package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SustentacaoOralService {

    public record SustentacaoInput(
            UUID processoId,
            UUID advogadoId,
            LocalDateTime sessaoAgendada,
            boolean inscricaoRealizada,
            boolean procuracaoNosAutos,
            boolean recursoAdmissivel,
            boolean embargosDeclaracaoExcluidos,
            boolean agravo
    ) {}

    public record SustentacaoResult(
            boolean habilitada,
            int tempoMinutos,
            List<String> impedimentos,
            String fundamentacao
    ) {}

    public SustentacaoResult avaliar(SustentacaoInput input) {
        List<String> impedimentos = new ArrayList<>();

        if (input.embargosDeclaracaoExcluidos()) {
            return new SustentacaoResult(false, 0,
                    List.of("Embargos de declaração não admitem sustentação oral (art. 937, CPC)."),
                    "Sustentação oral vedada para embargos de declaração.");
        }
        if (!input.recursoAdmissivel()) {
            impedimentos.add("Recurso não foi preliminarmente admitido — sustentação oral prejudicada.");
        }
        if (!input.inscricaoRealizada()) {
            impedimentos.add("Inscrição para sustentação oral não realizada antes do início da sessão.");
        }
        if (!input.procuracaoNosAutos()) {
            impedimentos.add("Procuração não localizada nos autos — representação não comprovada.");
        }

        boolean habilitada = impedimentos.isEmpty();
        int tempo = input.agravo() ? 15 : 15;

        return new SustentacaoResult(habilitada, habilitada ? tempo : 0,
                List.copyOf(impedimentos),
                habilitada
                        ? "Sustentação oral habilitada — " + tempo + " minutos (art. 937, CPC)."
                        : "Sustentação oral impedida: " + impedimentos.size() + " obstáculo(s).");
    }
}
