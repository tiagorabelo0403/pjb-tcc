package com.tcc.pjb.backend.core.processual.routing;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TribunalDeliberationCycleResolver {

    public TribunalDeliberationCycleProfile resolve(String tribunalCodigo,
                                                    TipoJustica tipoJustica,
                                                    GrauJurisdicao grau,
                                                    String specificOrgan,
                                                    String specializationAxis,
                                                    String sessionChannel,
                                                    String quorumHint) {
        String court = normalize(tribunalCodigo, "TRIBUNAL");
        String organ = normalize(specificOrgan, "ORGAO");
        String axis = normalize(specializationAxis, "GERAL");
        String deliberationMode = organ.contains("PLENARIO")
                ? "DELIBERACAO_PLENARIA"
                : organ.contains("SECAO")
                ? "DELIBERACAO_SECAO"
                : organ.contains("TURMA") || organ.contains("CAMARA")
                ? "DELIBERACAO_FRACIONARIA"
                : grau == GrauJurisdicao.SUPERIOR
                ? "DELIBERACAO_SUPERIOR"
                : "DELIBERACAO_COLEGIADA";
        String reviewerDesk = "REVISAO_COLEGIADA_" + axis + '_' + court;
        String divergenceDesk = organ.contains("PLENARIO")
                ? "DIVERGENCIA_PLENARIA_" + court
                : "DIVERGENCIA_ORGAO_" + court;
        String voteAuditDesk = "AUDITORIA_VOTOS_" + axis + '_' + court;
        String proclamationDesk = firstNonBlank(sessionChannel, "SESSAO_" + court) + "_PROCLAMACAO";
        String judgmentSequence = tipoJustica == TipoJustica.ELEITORAL
                ? "RELATOR_REVISAO_PROCLAMACAO"
                : organ.contains("PLENARIO")
                ? "RELATOR_VOTOS_PLENO_PROCLAMACAO"
                : quorumHint != null && quorumHint.contains("5")
                ? "RELATOR_REVISOR_COLETA_VOTOS_PROCLAMACAO"
                : "RELATOR_COLETA_VOTOS_PROCLAMACAO";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(deliberationMode);
        labels.add(judgmentSequence);
        if (tipoJustica != null) {
            labels.add(tipoJustica.name());
        }
        if (grau != null) {
            labels.add(grau.name());
        }
        if (organ.contains("PLENARIO")) {
            labels.add("PANEL_PLENARIO");
        }
        if (organ.contains("SECAO")) {
            labels.add("PANEL_SECAO");
        }
        if (organ.contains("TURMA") || organ.contains("CAMARA")) {
            labels.add("PANEL_FRACIONARIO");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("court", court);
        metadata.put("organ", organ);
        metadata.put("axis", axis);
        metadata.put("descriptor", deliberationMode + ':' + reviewerDesk + ':' + judgmentSequence);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new TribunalDeliberationCycleProfile(
                deliberationMode,
                reviewerDesk,
                divergenceDesk,
                voteAuditDesk,
                proclamationDesk,
                judgmentSequence,
                List.copyOf(labels),
                metadata
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
