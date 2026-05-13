package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.util.List;
import java.util.Set;

public record InstitutionalAccessLaneBlueprint(
        InstitutionalAccessLaneKind laneKind,
        String codigo,
        String nomeExibicao,
        InstitutionalNominationRole nominationRole,
        FuncaoOperacionalInstitucional funcaoOperacional,
        InstitutionalProcessProfile processProfile,
        InstitutionalEntryLandingPanel panel,
        InstitutionalTrustLevel trustFloor,
        Set<CapacidadeCaixaInstitucional> capacidadesPadrao,
        boolean requerStepUpMfa,
        boolean requerCertificadoICP,
        boolean requerRedeInstitucional,
        boolean permiteUsoRemotoAutorizado,
        List<String> restricoes,
        List<String> fundamentos
) {
    public InstitutionalAccessLaneBlueprint {
        capacidadesPadrao = capacidadesPadrao == null || capacidadesPadrao.isEmpty()
                ? java.util.EnumSet.noneOf(CapacidadeCaixaInstitucional.class)
                : java.util.EnumSet.copyOf(capacidadesPadrao);
        restricoes = restricoes == null ? List.of() : List.copyOf(restricoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
