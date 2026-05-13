package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record EmbargosDeclaracao(
        Set<EmbargosDeclaracaoGround> grounds,
        boolean efeitosInfringentesPretendidos,
        boolean contraDecisaoMonocratica,
        boolean interrompePrazoRecursalPrincipal) implements RecursalSpecies {

    public EmbargosDeclaracao {
        Objects.requireNonNull(grounds, "grounds");
        grounds = Set.copyOf(new LinkedHashSet<>(grounds));
        if (grounds.isEmpty()) {
            throw new IllegalArgumentException("Embargos de declaração exigem ao menos um fundamento típico");
        }
        if (efeitosInfringentesPretendidos && grounds.stream().noneMatch(EmbargosDeclaracaoGround::admiteEfeitoModificativo)) {
            throw new IllegalArgumentException("Efeitos infringentes exigem fundamento apto a modificar o julgado");
        }
    }

    @Override
    public String code() {
        return "EDCL";
    }

    @Override
    public String formalName() {
        return "Embargos de Declaração";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.EMBARGOS_DECLARACAO;
    }

    @Override
    public boolean sameCaseAutos() {
        return true;
    }

    @Override
    public boolean requiresCounterReasons() {
        return efeitosInfringentesPretendidos || grounds.stream().anyMatch(EmbargosDeclaracaoGround::admiteContraditorioPrevio);
    }

    @Override
    public boolean potentiallyRequiresPreparo() {
        return false;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return !contraDecisaoMonocratica;
    }
}
