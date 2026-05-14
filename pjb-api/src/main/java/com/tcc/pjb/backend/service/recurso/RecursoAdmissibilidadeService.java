package com.tcc.pjb.backend.service.recurso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursoAdmissibilidadeService {

    public record AdmissibilidadeInput(
            UUID processoId,
            RecursoProcessualTipo tipo,
            boolean legitimidadeRecorrente,
            boolean interesseRecursal,
            boolean cabimentoLegal,
            boolean singularidade,
            boolean representacaoRegular,
            boolean recursoProcuracaoJuntada,
            boolean decisaoRecorrivel
    ) {}

    public record AdmissibilidadeResult(
            boolean admissivel,
            List<String> obstaculos,
            String fundamentacao
    ) {}

    public AdmissibilidadeResult avaliar(AdmissibilidadeInput input) {
        List<String> obstaculos = new ArrayList<>();

        if (!input.decisaoRecorrivel()) {
            obstaculos.add("Decisão não comporta recurso na espécie (art. 994, CPC).");
        }
        if (!input.cabimentoLegal()) {
            obstaculos.add("Recurso não cabível para o ato impugnado (art. 994, CPC).");
        }
        if (!input.singularidade()) {
            obstaculos.add("Princípio da singularidade recursal violado — recurso já interposto.");
        }
        if (!input.legitimidadeRecorrente()) {
            obstaculos.add("Recorrente sem legitimidade (art. 996, CPC).");
        }
        if (!input.interesseRecursal()) {
            obstaculos.add("Ausência de interesse recursal — sucumbência não demonstrada (art. 996, CPC).");
        }
        if (!input.representacaoRegular()) {
            obstaculos.add("Representação processual irregular — procuração ausente ou viciada (art. 76, CPC).");
        }
        if (!input.recursoProcuracaoJuntada() && input.representacaoRegular()) {
            obstaculos.add("Instrumento de mandato não localizado nos autos.");
        }

        boolean admissivel = obstaculos.isEmpty();
        String fundamentacao = admissivel
                ? input.tipo() + " — admissível: todos os pressupostos atendidos."
                : input.tipo() + " — inadmissível: " + obstaculos.size() + " obstáculo(s) formal(is).";

        return new AdmissibilidadeResult(admissivel, List.copyOf(obstaculos), fundamentacao);
    }
}
