package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoInstrumento(
        boolean impugnaDecisaoInterlocutoria,
        boolean tutelaUrgenciaOuEvidencia,
        boolean versandoSobreCompetencia,
        boolean riscoLesaoGraveOuDificilReparacao) implements RecursalSpecies {

    public AgravoInstrumento {
        if (!impugnaDecisaoInterlocutoria) {
            throw new IllegalArgumentException("Agravo de instrumento exige decisão interlocutória agravável");
        }
        if (!tutelaUrgenciaOuEvidencia && !versandoSobreCompetencia && !riscoLesaoGraveOuDificilReparacao) {
            throw new IllegalArgumentException("Agravo de instrumento exige hipótese material compatível");
        }
    }

    @Override
    public String code() {
        return "AGINST";
    }

    @Override
    public String formalName() {
        return "Agravo de Instrumento";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_INSTRUMENTO;
    }

    @Override
    public boolean sameCaseAutos() {
        return false;
    }

    @Override
    public boolean requiresCounterReasons() {
        return true;
    }

    @Override
    public boolean potentiallyRequiresPreparo() {
        return true;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
