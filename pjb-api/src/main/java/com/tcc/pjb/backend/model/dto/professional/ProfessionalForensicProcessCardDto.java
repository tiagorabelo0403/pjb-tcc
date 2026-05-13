package com.tcc.pjb.backend.model.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicProcessCardDto(
        Long processoId,
        String numero,
        String tribunal,
        String uf,
        String comarca,
        String forum,
        String classeProcessual,
        String assunto,
        String tipoJustica,
        String ramoDireito,
        String sigilo,
        String actorClass,
        String accessBasis,
        boolean represented,
        boolean publicOnly,
        LocalDateTime ultimaMovimentacaoEm,
        String ultimaMovimentacao,
        List<String> capabilityCodes,
        List<String> allowedScopes,
        String detailRoute
) {
}
