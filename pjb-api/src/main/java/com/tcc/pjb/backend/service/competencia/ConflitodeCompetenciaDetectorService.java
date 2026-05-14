package com.tcc.pjb.backend.service.competencia;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConflitodeCompetenciaDetectorService {

    public enum TipoConflito {
        POSITIVO, NEGATIVO, PREVENTIVO_VIOLADO, FUNCIONAL_INCORRETO,
        TERRITORIAL_DUVIDOSO, ABSOLUTA_VIOLADA
    }

    public record ConflitoCandidato(
            UUID processoId,
            RitoProcessual rito,
            String orgaoAtual,
            String orgaoAlternativo,
            String motivoConflito,
            TipoConflito tipoConflito
    ) {}

    public record DiagnosticoConflito(
            UUID processoId,
            boolean temConflito,
            List<ConflitoCandidato> candidatos,
            String recomendacao,
            boolean requerDecisaoJurisdicional
    ) {}

    public DiagnosticoConflito detectar(UUID processoId, RitoProcessual rito,
            String orgaoDistribuido, List<String> orgaosConcorrentes,
            boolean prevencoViolada, boolean competenciaAbsolutaDuvidosa) {
        List<ConflitoCandidato> candidatos = new ArrayList<>();
        if (prevencoViolada) {
            candidatos.add(new ConflitoCandidato(processoId, rito, orgaoDistribuido,
                    orgaosConcorrentes.isEmpty() ? null : orgaosConcorrentes.get(0),
                    "Prevenção violada — processo conexo tramita em outro juízo.",
                    TipoConflito.PREVENTIVO_VIOLADO));
        }
        if (competenciaAbsolutaDuvidosa) {
            candidatos.add(new ConflitoCandidato(processoId, rito, orgaoDistribuido,
                    null,
                    "Competência absoluta (matéria ou pessoa) duvidosa para o rito " + rito + ".",
                    TipoConflito.ABSOLUTA_VIOLADA));
        }
        if (orgaosConcorrentes.size() > 1) {
            candidatos.add(new ConflitoCandidato(processoId, rito, orgaoDistribuido,
                    orgaosConcorrentes.get(0),
                    "Mais de um órgão julgador indicado para o mesmo processo — conflito positivo.",
                    TipoConflito.POSITIVO));
        }
        boolean requerDecisao = !candidatos.isEmpty();
        String recomendacao = requerDecisao
                ? "Suscitar conflito de competência ou informar magistrado para deliberação."
                : "Nenhum conflito identificado — competência confirmada.";
        return new DiagnosticoConflito(processoId, requerDecisao,
                List.copyOf(candidatos), recomendacao, requerDecisao);
    }
}
