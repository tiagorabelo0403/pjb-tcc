package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.util.List;

public record LaianeValidacaoSoberanaResponse(
        String status,
        String fonte,
        String politicaAssinatura,
        boolean cadeiaCustodiaElegivel,
        boolean assinaturaCompletaMaterializada,
        boolean rubricaDataHoraLocalPresentes,
        boolean classificacaoContextualCoerente,
        boolean certificadoEntradaVinculado,
        String papelAssinanteDetalhado,
        String ramoJustica,
        String instancia,
        String lotacaoAssinante,
        String sessionBindingHash,
        String replayShieldHash,
        String documentoAssinadoHash,
        List<LaianeRegraValidacaoResponse> regrasAplicadas
) {
}
