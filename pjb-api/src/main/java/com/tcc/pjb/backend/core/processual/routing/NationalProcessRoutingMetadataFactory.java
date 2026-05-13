package com.tcc.pjb.backend.core.processual.routing;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;

final class NationalProcessRoutingMetadataFactory {

    private final NationalProcessRoutingSupport support;

    NationalProcessRoutingMetadataFactory(NationalProcessRoutingSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    LinkedHashMap<String, Object> build(NationalCompetenceMatrix competencia,
                                        TerritorialRoutingProfile territorial,
                                        RelationalRoutingProfile relational,
                                        FracionaryOrganRoutingProfile fracionary,
                                        ProceduralCoverageProfile coverage,
                                        String distributionMode,
                                        String preventionMode,
                                        String mesaTriagem,
                                        String orgaoJulgador,
                                        String fila,
                                        String unidade,
                                        BigDecimal limiteJuizado,
                                        boolean conciliacaoObrigatoria,
                                        String specializationAxis,
                                        String allocationStrategy,
                                        String linkageMode,
                                        String competenceEnvelope,
                                        String routingRiskLevel,
                                        String suggestedDeskProfile,
                                        List<String> reviewChecklist) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(competencia.toMap());
        metadata.put("territorial", territorial.toMap());
        metadata.put("relational", relational.toMap());
        metadata.put("fracionary", fracionary.toMap());
        metadata.put("coverage", coverage.toMap());
        metadata.put("distributionMode", distributionMode);
        metadata.put("preventionMode", preventionMode);
        metadata.put("mesaTriagem", mesaTriagem);
        metadata.put("orgaoJulgadorSugerido", orgaoJulgador);
        metadata.put("filaDistribuicao", fila);
        metadata.put("unidadeJudiciariaCodigo", unidade);
        metadata.put("foroSugerido", territorial.foro());
        metadata.put("reviewChecklist", List.copyOf(reviewChecklist));
        metadata.put("limiteJuizado", limiteJuizado);
        metadata.put("conciliacaoObrigatoria", conciliacaoObrigatoria);
        metadata.put("specializationAxis", specializationAxis);
        metadata.put("allocationStrategy", allocationStrategy);
        metadata.put("linkageMode", linkageMode);
        metadata.put("attachmentMode", relational.attachmentMode());
        metadata.put("registryBucket", relational.registryBucket());
        metadata.put("triageBucket", relational.triageBucket());
        metadata.put("competenceEnvelope", competenceEnvelope);
        metadata.put("routingRiskLevel", routingRiskLevel);
        metadata.put("suggestedDeskProfile", suggestedDeskProfile);
        metadata.put("territorialRegistry", territorial.metadata().get("territorialRegistry"));
        metadata.put("forumDistributionCluster", support.metadataString(territorial.toMap(), "forumRegistry.distributionCluster"));
        metadata.put("territorialSupportDesk", support.metadataString(territorial.toMap(), "forumRegistry.supportDesk"));
        metadata.put("internalOrganLabel", support.metadataString(fracionary.toMap(), "internalOrgan.specificOrgan"));
        metadata.put("internalSecretariatDesk", support.metadataString(fracionary.toMap(), "internalOrgan.secretariatDesk"));
        metadata.put("uniformizationHub", support.metadataString(fracionary.toMap(), "bridge.uniformizationHub"));
        metadata.put("coverageDescriptor", coverage.descriptor());
        metadata.put("materialityAxis", coverage.materialityAxis());
        metadata.put("forumScope", coverage.forumScope());
        metadata.put("territorialAnchor", coverage.territorialAnchor());
        metadata.put("admissibilityChannel", coverage.admissibilityChannel());
        metadata.put("executionTrack", coverage.executionTrack());
        metadata.put("recursalTrack", coverage.recursalTrack());
        metadata.put("preventionAnchor", coverage.preventionAnchor());
        metadata.put("concurrencyEnvelope", coverage.concurrencyEnvelope());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return metadata;
    }
}
