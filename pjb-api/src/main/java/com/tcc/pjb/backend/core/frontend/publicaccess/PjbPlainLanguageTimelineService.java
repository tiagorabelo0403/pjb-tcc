package com.tcc.pjb.backend.core.frontend.publicaccess;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PjbPlainLanguageTimelineService {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("CONCLUSOS", "O processo foi enviado para análise do gabinete."),
            Map.entry("DECISAO", "Houve uma decisão no processo."),
            Map.entry("INTIMACAO", "Alguém foi comunicado oficialmente para tomar ciência ou agir."),
            Map.entry("AUDIENCIA", "Há ato de audiência ou sessão relacionado ao processo."),
            Map.entry("SENTENCA", "O juiz apresentou uma sentença."),
            Map.entry("RECURSO", "O processo entrou em etapa de recurso."),
            Map.entry("ARQUIVAMENTO", "O processo foi encaminhado para encerramento ou guarda."),
            Map.entry("DISTRIBUICAO", "O processo foi direcionado para uma unidade judicial.")
    );

    public PjbPublicTimelineEntry explain(String movementCode,
                                          String technicalLabel,
                                          Instant occurredAt,
                                          boolean visibleToPublic) {
        String token = normalize(firstNonBlank(movementCode, technicalLabel));
        String label = LABELS.entrySet().stream()
                .filter(entry -> token.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Houve uma atualização no andamento do processo.");
        String next = nextStep(token);
        return new PjbPublicTimelineEntry(movementCode, technicalLabel, label, next, occurredAt, visibleToPublic);
    }

    private String nextStep(String token) {
        if (token.contains("INTIMACAO")) {
            return "verifique o prazo e a necessidade de resposta";
        }
        if (token.contains("CONCLUSOS")) {
            return "aguarde despacho, decisão ou sentença";
        }
        if (token.contains("AUDIENCIA")) {
            return "confira data, local, link e documentos necessários";
        }
        if (token.contains("RECURSO")) {
            return "acompanhe análise de admissibilidade e remessa";
        }
        return "acompanhe os próximos movimentos no painel público";
    }

    private String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U');
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
