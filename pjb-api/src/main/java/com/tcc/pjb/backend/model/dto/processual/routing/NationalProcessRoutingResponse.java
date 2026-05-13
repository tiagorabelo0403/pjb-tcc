package com.tcc.pjb.backend.model.dto.processual.routing;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record NationalProcessRoutingResponse(
        RitoProcessual rito,
        RamoDireito ramoDireito,
        GrauJurisdicao grau,
        TipoJustica tipoJustica,
        String tribunalCodigo,
        String tribunalNome,
        String ramoJusticaNacional,
        String sistemaPrimario,
        String sistemaFallback,
        String instancia,
        String orgaoJulgadorSugerido,
        String unidadeJudiciariaCodigo,
        String filaDistribuicao,
        boolean sigiloPadrao,
        boolean admiteJuizado,
        boolean conciliacaoObrigatoria,
        int prazoTriagemHoras,
        BigDecimal limiteJuizado,
        String cidadeSugerida,
        String comarcaSugerida,
        String foroSugerido,
        String secaoJudiciariaSugerida,
        String subsecaoJudiciariaSugerida,
        String circunscricaoJudiciariaSugerida,
        String territorialMode,
        String preventionMode,
        String distributionMode,
        String specializationAxis,
        String allocationStrategy,
        String linkageMode,
        String competenceEnvelope,
        String routingRiskLevel,
        String suggestedDeskProfile,
        String mesaTriagem,
        List<String> alertas,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public NationalProcessRoutingResponse {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public boolean requiresHumanReview() {
        return !"AUTO_DIRETA".equals(distributionMode)
                || "CRITICO".equals(routingRiskLevel)
                || !reviewChecklist.isEmpty();
    }

    public String territorialSnapshotLabel() {
        StringBuilder sb = new StringBuilder();
        append(sb, foroSugerido);
        append(sb, subsecaoJudiciariaSugerida);
        append(sb, comarcaSugerida);
        append(sb, cidadeSugerida);
        append(sb, circunscricaoJudiciariaSugerida);
        return sb.isEmpty() ? territorialMode : sb.toString();
    }

    public String relationalSnapshotLabel() {
        return firstNonBlank(metadataString("relational.linkageMode"), linkageMode, "AUTONOMA");
    }

    public String fracionarySnapshotLabel() {
        return firstNonBlank(metadataString("fracionary.orgaoFracionario"), orgaoJulgadorSugerido, instancia, "JUIZO_ORIGINARIO");
    }

    public String forumRegistryLabel() {
        return firstNonBlank(metadataString("territorial.forumRegistry.primaryForum"), metadataString("territorial.forumRegistry.judicialDistrictLabel"), territorialSnapshotLabel());
    }

    public String internalOrganLabel() {
        return firstNonBlank(metadataString("fracionary.internalOrgan.specificOrgan"), metadataString("fracionary.internalOrgan.macroOrgan"), fracionarySnapshotLabel());
    }

    public String coverageLabel() {
        return firstNonBlank(metadataString("coverage.descriptor"), metadataString("coverage.materialityAxis"), competenceEnvelope, specializationAxis, "COBERTURA_NAO_CLASSIFICADA");
    }

    public String routingSummary() {
        return firstNonBlank(tribunalCodigo, "TRIBUNAL")
                + " | " + firstNonBlank(instancia, "INSTANCIA")
                + " | " + firstNonBlank(metadataString("coverage.materialityAxis"), specializationAxis, "EIXO")
                + " | " + firstNonBlank(allocationStrategy, "ALOCACAO")
                + " | " + firstNonBlank(fracionarySnapshotLabel(), "ORGAO")
                + " | " + firstNonBlank(territorialSnapshotLabel(), "TERRITORIO");
    }

    public Map<String, Object> flatView() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("summary", routingSummary());
        out.put("requiresHumanReview", requiresHumanReview());
        out.put("territorialSnapshotLabel", territorialSnapshotLabel());
        out.put("specializationAxis", specializationAxis);
        out.put("allocationStrategy", allocationStrategy);
        out.put("linkageMode", linkageMode);
        out.put("relationalSnapshotLabel", relationalSnapshotLabel());
        out.put("fracionarySnapshotLabel", fracionarySnapshotLabel());
        out.put("forumRegistryLabel", forumRegistryLabel());
        out.put("internalOrganLabel", internalOrganLabel());
        out.put("competenceEnvelope", competenceEnvelope);
        out.put("coverageLabel", coverageLabel());
        out.put("coverageDescriptor", metadataString("coverage.descriptor"));
        out.put("coverageJusticeTrack", metadataString("coverage.justiceTrack"));
        out.put("coverageTribunalTier", metadataString("coverage.tribunalTier"));
        out.put("coverageRiteFamily", metadataString("coverage.riteFamily"));
        out.put("coverageMaterialityAxis", metadataString("coverage.materialityAxis"));
        out.put("coverageForumScope", metadataString("coverage.forumScope"));
        out.put("coverageTerritorialAnchor", metadataString("coverage.territorialAnchor"));
        out.put("coverageAdmissibilityChannel", metadataString("coverage.admissibilityChannel"));
        out.put("coverageExecutionTrack", metadataString("coverage.executionTrack"));
        out.put("coverageRecursalTrack", metadataString("coverage.recursalTrack"));
        out.put("coveragePreventionAnchor", metadataString("coverage.preventionAnchor"));
        out.put("coverageConcurrencyEnvelope", metadataString("coverage.concurrencyEnvelope"));
        out.put("routingRiskLevel", routingRiskLevel);
        out.put("suggestedDeskProfile", suggestedDeskProfile);
        out.put("attachmentMode", metadataString("relational.attachmentMode"));
        out.put("registryBucket", metadataString("relational.registryBucket"));
        out.put("bindingStrength", metadataString("relational.binding.bindingStrength"));
        out.put("bindingFingerprint", metadataString("relational.binding.preventionFingerprint"));
        out.put("venueMode", metadataString("territorial.venueMesh.venueMode"));
        out.put("venueConfidence", metadataString("territorial.venueMesh.territorialConfidence"));
        out.put("territorialRegistry", metadataString("territorial.territorialRegistry"));
        out.put("forumDistributionCluster", metadataString("territorial.forumDistributionCluster"));
        out.put("territorialSupportDesk", metadataString("territorial.territorialSupportDesk"));
        out.put("uniformizationHub", metadataString("fracionary.bridge.uniformizationHub"));
        out.put("collegiateDesk", metadataString("fracionary.catalog.secretariatDesk"));
        out.put("internalOrganDesk", metadataString("fracionary.internalOrgan.secretariatDesk"));
        out.put("internalGabineteDesk", metadataString("fracionary.internalOrgan.gabineteDesk"));
        out.put("internalChamberLabel", metadataString("fracionary.internalOrgan.chamber.chamberLabel"));
        out.put("internalRelatoriaDesk", metadataString("fracionary.internalOrgan.chamber.relatoriaDesk"));
        out.put("internalSessionRoom", metadataString("fracionary.internalOrgan.chamber.sessionRoom"));
        out.put("internalPreventionClass", metadataString("fracionary.internalOrgan.chamber.preventionClass"));
        out.put("internalSessionBlock", metadataString("fracionary.internalOrgan.sessionTopology.sessionBlock"));
        out.put("internalPublicationFlow", metadataString("fracionary.internalOrgan.sessionTopology.publicationFlow"));
        out.put("internalReviewDesk", metadataString("fracionary.internalOrgan.sessionTopology.internalReviewDesk"));
        out.put("internalPanelSizeHint", metadataString("fracionary.internalOrgan.sessionTopology.panelSizeHint"));
        out.put("internalCadenceHint", metadataString("fracionary.internalOrgan.sessionTopology.cadenceHint"));
        out.put("internalSpecificAlias", metadataString("fracionary.internalOrgan.specificOrganProfile.organAlias"));
        out.put("internalPublicationDesk", metadataString("fracionary.internalOrgan.specificOrganProfile.publicationDesk"));
        out.put("internalPublicationQueue", metadataString("fracionary.internalOrgan.specificOrganProfile.publicationQueue"));
        out.put("internalSpecificReviewDesk", metadataString("fracionary.internalOrgan.specificOrganProfile.reviewDesk"));
        out.put("internalTopologyDescriptor", metadataString("fracionary.internalOrgan.specificOrganProfile.topologyDescriptor"));
        out.put("internalPanelComposition", metadataString("fracionary.internalOrgan.panelComposition.panelCompositionLabel"));
        out.put("internalVoteCollectionMode", metadataString("fracionary.internalOrgan.panelComposition.voteCollectionMode"));
        out.put("internalPublicationSequence", metadataString("fracionary.internalOrgan.panelComposition.publicationSequence"));
        out.put("internalClerkCluster", metadataString("fracionary.internalOrgan.panelComposition.clerkCluster"));
        out.put("internalDeliberationMode", metadataString("fracionary.internalOrgan.deliberationCycle.deliberationMode"));
        out.put("internalReviewerDesk", metadataString("fracionary.internalOrgan.deliberationCycle.reviewerDesk"));
        out.put("internalDivergenceDesk", metadataString("fracionary.internalOrgan.deliberationCycle.divergenceDesk"));
        out.put("internalVoteAuditDesk", metadataString("fracionary.internalOrgan.deliberationCycle.voteAuditDesk"));
        out.put("internalProclamationDesk", metadataString("fracionary.internalOrgan.deliberationCycle.proclamationDesk"));
        out.put("internalJudgmentSequence", metadataString("fracionary.internalOrgan.deliberationCycle.judgmentSequence"));
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Map.copyOf(out);
    }

    public String metadataString(String dottedPath) {
        return metadataStringByPath(dottedPath);
    }

    private String metadataStringByPath(String dottedPath) {

        if (metadata == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = metadata;
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }
        return String.valueOf(current);
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" / ");
        }
        sb.append(value.trim());
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
}
