package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "Consultoria de decisão judicial assistida pelo Laiane")
public record LaianeJudicialDecisionAdvisoryResponse(
        @Schema(description = "Código do template de decisão judicial aplicável",
                example = "SENTENCA_PENAL_CONDENATORIA") JudicialDecisionTemplateCode templateCode,
        @Schema(description = "Modo de consultoria. Nesta versão, sempre \"ADVISORY_DRAFT_ONLY\" — a Laiane produz apenas minuta assistida sujeita a revisão humana integral, sem modos alternativos de consultoria implementados.",
                example = "ADVISORY_DRAFT_ONLY",
                allowableValues = {"ADVISORY_DRAFT_ONLY"}) String advisoryMode,
        @Schema(description = "Sempre true, por política de segurança: toda consultoria de decisão judicial exige revisão humana antes da publicação, independente do template ou do caso", example = "true") boolean reviewRequired,
        @Schema(description = "Sempre true, por política de segurança: a Laiane nunca publica decisão automaticamente — apenas gera minuta assistida sob revisão judicial integral", example = "true") boolean publicationLocked,
        @Size(max = 5000)
        @Schema(description = "Sumário do raciocínio jurídico da consultoria",
                example = "Tipicidade confirmada; dosimetria sugere regime inicial semiaberto") String rationaleSummary,
        @Size(max = 50)
        @Schema(description = "Fundamentos legais identificados (máx. 50)") List<String> legalAnchors,
        @Size(max = 50)
        @Schema(description = "Checklist de raciocínio jurídico (máx. 50)") List<String> reasoningChecklist,
        @Size(max = 30)
        @Schema(description = "Fatos pendentes de análise (máx. 30)") List<String> pendingFacts,
        @Size(max = 10_000)
        @Schema(description = "Base do dispositivo da decisão gerada pelo Laiane",
                example = "CONDENO o réu Fulano de Tal às penas do artigo...") String dispositiveBase,
        @Schema(description = "Variáveis preenchíveis na minuta da decisão") Map<String, String> fillableVariables,
        @Schema(description = "Metadados adicionais da consultoria") Map<String, Object> metadata
) {
    public LaianeJudicialDecisionAdvisoryResponse {
        legalAnchors = legalAnchors == null ? List.of() : List.copyOf(legalAnchors);
        reasoningChecklist = reasoningChecklist == null ? List.of() : List.copyOf(reasoningChecklist);
        pendingFacts = pendingFacts == null ? List.of() : List.copyOf(pendingFacts);
        fillableVariables = fillableVariables == null ? Map.of() : Map.copyOf(fillableVariables);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Map<String, Object> toMap() {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("templateCode", templateCode != null ? templateCode.name() : null);
        out.put("advisoryMode", advisoryMode);
        out.put("reviewRequired", reviewRequired);
        out.put("publicationLocked", publicationLocked);
        out.put("rationaleSummary", rationaleSummary);
        out.put("legalAnchors", legalAnchors);
        out.put("reasoningChecklist", reasoningChecklist);
        out.put("pendingFacts", pendingFacts);
        out.put("dispositiveBase", dispositiveBase);
        out.put("fillableVariables", fillableVariables);
        out.put("metadata", metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return java.util.Map.copyOf(out);
    }
}
