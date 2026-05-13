package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProceduralExecutiveExplanationMessages {

    private static final Map<ProceduralExecutiveExplanationCode, String> DEFAULT_MESSAGES = Map.ofEntries(
            Map.entry(ProceduralExecutiveExplanationCode.AXIS_CONSENSUS_HIGH, "Os eixos canônicos convergem com baixa tensão semântica e boa aderência estrutural."),
            Map.entry(ProceduralExecutiveExplanationCode.AXIS_CONFLICT_REVIEW, "Há conflito material entre os eixos do caso e a revisão humana permanece recomendada."),
            Map.entry(ProceduralExecutiveExplanationCode.AUTOMATION_SAFE_ROUTE, "O caso admite automação segura apenas para atos preparatórios e roteamento assistido."),
            Map.entry(ProceduralExecutiveExplanationCode.AUTOMATION_ASSISTED_ONLY, "O caso admite apoio inteligente, mas a decisão final deve permanecer assistida."),
            Map.entry(ProceduralExecutiveExplanationCode.AUTOMATION_HUMAN_GATE, "O caso exige gate humano obrigatório antes de qualquer ato decisório ou terminal."),
            Map.entry(ProceduralExecutiveExplanationCode.NATUREZA_JURIDICA_SIGNAL, "A natureza jurídica canônica reforça o enquadramento predominante do procedimento."),
            Map.entry(ProceduralExecutiveExplanationCode.SENSITIVE_DOMAIN_RESTRICTION, "O domínio processual é sensível e reduz a latitude de automação."),
            Map.entry(ProceduralExecutiveExplanationCode.MISSING_FOUNDATIONAL_SIGNAL, "Persistem lacunas de lastro fático ou normativo para consolidação automática."),
            Map.entry(ProceduralExecutiveExplanationCode.QUALITY_LOW_DETERMINISM, "O determinismo inferencial está abaixo do nível adequado para ação autônoma."),
            Map.entry(ProceduralExecutiveExplanationCode.QUALITY_STRONG_EVIDENCE, "A evidência estruturada e textual sustenta a recomendação em nível elevado."),
            Map.entry(ProceduralExecutiveExplanationCode.ARCHIVE_CONCEALMENT_RECOMMENDED, "O pós-arquivamento recomenda ocultação operacional controlada, sem supressão material do acervo."),
            Map.entry(ProceduralExecutiveExplanationCode.ARCHIVE_PARTY_GATE_REQUIRED, "A reabertura de visibilidade deve passar por autenticação qualificada da parte ou controle institucional."),
            Map.entry(ProceduralExecutiveExplanationCode.JUDICIAL_DRAFT_AGREEMENT_TEMPLATE, "Há base para minuta assistida de homologação de acordo, sujeita à validação judicial integral."),
            Map.entry(ProceduralExecutiveExplanationCode.JUDICIAL_DRAFT_DESISTENCE_TEMPLATE, "Há base para minuta assistida de homologação de desistência ou extinção sem mérito, sujeita à validação judicial integral.")
    );

    private ProceduralExecutiveExplanationMessages() {
    }

    public static String resolve(ProceduralExecutiveExplanationCode code, String detail) {
        String base = DEFAULT_MESSAGES.getOrDefault(code, "Explicação executiva indisponível.");
        if (detail == null || detail.isBlank()) {
            return base;
        }
        return base + ' ' + normalize(detail);
    }

    public static Map<String, Object> toMap(ProceduralExecutiveExplanationCode code, String detail) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code != null ? code.name() : null);
        out.put("message", resolve(code, detail));
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private static String normalize(String text) {
        return Objects.toString(text, "").trim().replaceAll("\\s+", " ");
    }
}
