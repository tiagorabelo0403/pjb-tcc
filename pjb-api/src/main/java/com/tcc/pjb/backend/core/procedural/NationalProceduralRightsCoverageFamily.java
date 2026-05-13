package com.tcc.pjb.backend.core.procedural;

import java.util.List;

public record NationalProceduralRightsCoverageFamily(
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
