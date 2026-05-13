package com.tcc.pjb.backend.model.dto.processual.cobertura;

import java.util.List;

public record ProcessoProceduralCoverageFamilyResponse(
        String familyCode,
        String displayName,
        int totalRitos,
        boolean segredoPadrao,
        boolean exigeMinisterioPublico,
        boolean admiteConciliacao,
        List<String> justiceTracks,
        List<String> markers
) {
}
