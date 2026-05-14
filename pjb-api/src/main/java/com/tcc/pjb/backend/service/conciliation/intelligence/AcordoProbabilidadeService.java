package com.tcc.pjb.backend.service.conciliation.intelligence;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcordoProbabilidadeService {

    public record ProbabilidadeInput(
            UUID processoId,
            RitoProcessual rito,
            boolean historicoConciliacao,
            boolean parteRepresentada,
            boolean valorBaixo,
            boolean causaRepetitiva,
            boolean audienciaMarcada
    ) {}

    public record ProbabilidadeAcordo(
            UUID processoId,
            int probabilidadePercent,
            String classificacao,
            String justificativa
    ) {}

    public ProbabilidadeAcordo calcular(ProbabilidadeInput input) {
        int prob = calcularBase(input.rito());
        if (input.historicoConciliacao()) prob += 20;
        if (input.parteRepresentada()) prob += 10;
        if (input.valorBaixo()) prob += 15;
        if (input.causaRepetitiva()) prob += 10;
        if (input.audienciaMarcada()) prob += 5;
        prob = Math.min(prob, 95);

        String classificacao = prob >= 70 ? "ALTA" : prob >= 40 ? "MEDIA" : "BAIXA";
        String justificativa = construir(input, prob);

        return new ProbabilidadeAcordo(input.processoId(), prob, classificacao, justificativa);
    }

    private int calcularBase(RitoProcessual rito) {
        if (rito.name().contains("JUIZADO")) return 50;
        if (rito.name().contains("TRABALHISTA") || rito.name().contains("TRABALHIST")) return 45;
        if (rito.isFamiliaSucessoes()) return 40;
        return 25;
    }

    private String construir(ProbabilidadeInput input, int prob) {
        return "Probabilidade estimada: " + prob + "% — rito " + input.rito().name()
                + (input.historicoConciliacao() ? " | Histórico favorável de acordos" : "");
    }
}
