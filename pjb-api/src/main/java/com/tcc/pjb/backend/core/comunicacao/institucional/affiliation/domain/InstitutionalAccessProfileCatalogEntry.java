package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.util.List;
import java.util.Set;

public record InstitutionalAccessProfileCatalogEntry(
        String codigo,
        String nomeExibicao,
        InstitutionalEntryMode entryMode,
        InstitutionalNominationRole nominationRole,
        InstitutionalProcessProfile processProfile,
        InstitutionalEntryLandingPanel panel,
        InstitutionalTrustLevel trustFloor,
        Set<CapacidadeCaixaInstitucional> capacidadesPadrao,
        List<String> restricoes,
        List<String> fundamentos
) {
}
