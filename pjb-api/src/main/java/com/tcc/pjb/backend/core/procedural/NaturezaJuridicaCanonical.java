package com.tcc.pjb.backend.core.procedural;

import java.util.EnumSet;
import java.util.Set;

public enum NaturezaJuridicaCanonical {
    CONHECIMENTO_DECLARATORIA(
            "Conhecimento declaratória",
            EnumSet.of(NaturezaJuridicaQualifier.INDIVIDUAL, NaturezaJuridicaQualifier.CONTENCIOSA)
    ),
    CONHECIMENTO_CONDENATORIA(
            "Conhecimento condenatória",
            EnumSet.of(NaturezaJuridicaQualifier.INDIVIDUAL, NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.SATISFATIVA)
    ),
    CONHECIMENTO_CONSTITUTIVA(
            "Conhecimento constitutiva",
            EnumSet.of(NaturezaJuridicaQualifier.INDIVIDUAL, NaturezaJuridicaQualifier.CONTENCIOSA)
    ),
    EXECUTIVA(
            "Executiva",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.EXECUTIVA, NaturezaJuridicaQualifier.PATRIMONIAL)
    ),
    CAUTELAR(
            "Cautelar",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.URGENTE, NaturezaJuridicaQualifier.PREVENTIVA)
    ),
    TUTELA_PROVISORIA(
            "Tutela provisória",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.URGENTE, NaturezaJuridicaQualifier.PREVENTIVA, NaturezaJuridicaQualifier.SATISFATIVA)
    ),
    MANDAMENTAL(
            "Mandamental",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.MANDAMENTAL, NaturezaJuridicaQualifier.URGENTE)
    ),
    HOMOLOGATORIA(
            "Homologatória",
            EnumSet.of(NaturezaJuridicaQualifier.VOLUNTARIA)
    ),
    JURISDICAO_VOLUNTARIA(
            "Jurisdição voluntária",
            EnumSet.of(NaturezaJuridicaQualifier.VOLUNTARIA)
    ),
    SANCIONATORIA(
            "Sancionatória",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA, NaturezaJuridicaQualifier.PUBLICA, NaturezaJuridicaQualifier.REPRESSIVA)
    ),
    INVESTIGATIVA(
            "Investigativa",
            EnumSet.of(NaturezaJuridicaQualifier.PUBLICA, NaturezaJuridicaQualifier.PREVENTIVA)
    ),
    IMPUGNATIVA(
            "Impugnativa",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA)
    ),
    REVISIONAL(
            "Revisional",
            EnumSet.of(NaturezaJuridicaQualifier.CONTENCIOSA)
    ),
    ESTRUTURAL_COLETIVA(
            "Estrutural coletiva",
            EnumSet.of(NaturezaJuridicaQualifier.COLETIVA, NaturezaJuridicaQualifier.ESTRUTURAL, NaturezaJuridicaQualifier.PUBLICA)
    );

    private final String label;
    private final EnumSet<NaturezaJuridicaQualifier> baselineQualifiers;

    NaturezaJuridicaCanonical(String label, EnumSet<NaturezaJuridicaQualifier> baselineQualifiers) {
        this.label = label;
        this.baselineQualifiers = baselineQualifiers.clone();
    }

    public String label() {
        return label;
    }

    public Set<NaturezaJuridicaQualifier> baselineQualifiers() {
        return EnumSet.copyOf(baselineQualifiers);
    }

    public boolean isUrgentByNature() {
        return baselineQualifiers.contains(NaturezaJuridicaQualifier.URGENTE)
                || this == MANDAMENTAL
                || this == CAUTELAR
                || this == TUTELA_PROVISORIA;
    }

    public boolean isVoluntaryByNature() {
        return this == JURISDICAO_VOLUNTARIA || this == HOMOLOGATORIA;
    }

    public boolean isCollectiveByNature() {
        return baselineQualifiers.contains(NaturezaJuridicaQualifier.COLETIVA) || this == ESTRUTURAL_COLETIVA;
    }

    public boolean suggestsConfidentiality() {
        return this == INVESTIGATIVA || this == SANCIONATORIA;
    }

    public boolean isEnforcementOriented() {
        return this == EXECUTIVA || baselineQualifiers.contains(NaturezaJuridicaQualifier.EXECUTIVA);
    }
}
