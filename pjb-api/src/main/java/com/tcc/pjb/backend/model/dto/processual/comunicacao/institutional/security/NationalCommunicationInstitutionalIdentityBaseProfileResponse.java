package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record NationalCommunicationInstitutionalIdentityBaseProfileResponse(
        String identityCode,
        String tipoUsuarioBase,
        boolean possuiFluxoDireto,
        String entryModePreferencial,
        String processProfileBase,
        String painelBase,
        String trustFloorBase,
        boolean exigeNomeacaoInstitucionalParaAtos,
        List<String> fundamentos
) {
}
