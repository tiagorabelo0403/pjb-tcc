package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.time.LocalDate;
import java.time.LocalTime;

public record LaianeAssinaturaQualificadaResponse(
        String envelopeId,
        String assinaturaHash,
        String conteudoBaseHash,
        String documentoAssinadoHash,
        boolean rubrica,
        LocalDate data,
        LocalTime hora,
        String local,
        String signatario,
        String papelAssinante,
        String papelAssinanteDetalhado,
        String segmentoInstitucional,
        String ramoJustica,
        String esferaInstitucional,
        String instancia,
        String orgaoAssinante,
        String lotacaoAssinante,
        String registroProfissional,
        boolean coerenciaCertificadoUsuario,
        String sessionBindingHash,
        String replayShieldHash,
        LaianeValidacaoSoberanaResponse validacaoSoberana
) {
}
