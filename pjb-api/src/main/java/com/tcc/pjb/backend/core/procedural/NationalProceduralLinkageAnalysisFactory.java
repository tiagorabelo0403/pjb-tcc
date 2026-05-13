package com.tcc.pjb.backend.core.procedural;

import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.bool;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.containsAny;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.firstNonBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.isBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.normalize;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.text;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralLinkageAnalysisFactory {

    private static final Pattern CNJ_NUMBER_PATTERN = Pattern.compile("\\b\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}\\b");

    private final NationalProceduralForumAllocationMessages messages;

    public NationalProceduralLinkageAnalysisFactory(NationalProceduralForumAllocationMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    public NationalProceduralLinkageAnalysis resolve(Map<String, Object> payload, String corpus) {
        LinkedHashSet<String> related = new LinkedHashSet<>();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        Object relatedRaw = payload.get("processosRelacionados");
        if (relatedRaw instanceof List<?> list) {
            for (Object entry : list) {
                String candidate = text(entry);
                if (!isBlank(candidate)) {
                    related.add(candidate);
                }
            }
        } else if (relatedRaw != null) {
            for (String token : String.valueOf(relatedRaw).split("[,;\\n]")) {
                if (!isBlank(token)) {
                    related.add(token.trim());
                }
            }
        }
        Matcher matcher = CNJ_NUMBER_PATTERN.matcher(firstNonBlank(corpus, ""));
        while (matcher.find()) {
            related.add(matcher.group());
        }
        String normalizedCorpus = normalize(corpus);
        boolean preventionHint = bool(payload.get("prevention"))
                || containsAny(text(payload.get("preventionMode")), "PREVENCAO")
                || containsAny(normalizedCorpus, "PREVENCAO", "JUIZO PREVENTO", "DISTRIBUICAO POR PREVENCAO");
        boolean continenciaHint = bool(payload.get("continencia"))
                || containsAny(text(payload.get("linkageMode")), "CONTINENCIA")
                || containsAny(normalizedCorpus, "CONTINENCIA");
        boolean conexaoHint = bool(payload.get("conexao"))
                || containsAny(text(payload.get("linkageMode")), "CONEXAO")
                || containsAny(normalizedCorpus, "CONEXAO", "PROCESSO CONEXO");
        boolean dependenciaHint = bool(payload.get("dependencia"))
                || containsAny(text(payload.get("linkageMode")), "DEPENDENCIA")
                || containsAny(normalizedCorpus, "DEPENDENCIA", "DISTRIBUICAO POR DEPENDENCIA");
        String preventionMode = preventionHint
                ? firstNonBlank(text(payload.get("preventionMode")), related.isEmpty() ? "PREVENCAO_SINALIZADA" : "PREVENCAO_PROCESSO:" + related.iterator().next())
                : text(payload.get("preventionMode"));
        String linkageMode;
        if (continenciaHint) {
            linkageMode = "CONTINENCIA";
            reasons.add(messages.linkageContinenciaReason());
        } else if (preventionHint) {
            linkageMode = "PREVENCAO";
            reasons.add(messages.linkagePreventionReason());
        } else if (dependenciaHint) {
            linkageMode = "DEPENDENCIA";
            reasons.add(messages.linkageDependenciaReason());
        } else if (conexaoHint) {
            linkageMode = "CONEXAO";
            reasons.add(messages.linkageConexaoReason());
        } else {
            linkageMode = related.isEmpty() ? "NENHUM_SINAL" : "PROCESSOS_RELACIONADOS";
            if (!related.isEmpty()) {
                reasons.add(messages.linkageRelatedReason());
            }
        }
        if (!related.isEmpty()) {
            reasons.add(messages.linkageRelatedCountReason(related.size()));
        }
        return new NationalProceduralLinkageAnalysis(
                preventionMode,
                linkageMode,
                List.copyOf(related),
                List.copyOf(reasons)
        );
    }
}
