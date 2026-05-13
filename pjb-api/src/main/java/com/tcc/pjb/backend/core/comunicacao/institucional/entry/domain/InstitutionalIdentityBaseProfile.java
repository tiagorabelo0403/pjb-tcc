package com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain;

import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;

public record InstitutionalIdentityBaseProfile(
        String identityCode,
        TipoUsuario tipoUsuarioBase,
        boolean possuiFluxoDireto,
        InstitutionalEntryMode entryModePreferencial,
        InstitutionalProcessProfile processProfileBase,
        InstitutionalEntryLandingPanel painelBase,
        InstitutionalTrustLevel trustFloorBase,
        boolean exigeNomeacaoInstitucionalParaAtos,
        List<String> fundamentos
) {
    public InstitutionalIdentityBaseProfile {
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
