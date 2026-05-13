package com.tcc.pjb.backend.model.dto.professional;

import com.tcc.pjb.backend.model.dto.publico.PublicDocumentoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicPartesDTO;
import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalForensicProcessDetailResponse(
        LocalDateTime generatedAt,
        boolean allowed,
        String panelMode,
        String actorClass,
        String accessBasis,
        String accessReason,
        boolean represented,
        boolean publicOnly,
        boolean requiresStepUp,
        Long processoId,
        String numero,
        String tribunal,
        String uf,
        String comarca,
        String forum,
        String tipoJustica,
        String ramoDireito,
        String classeProcessual,
        String assunto,
        String sigilo,
        PublicPartesDTO partes,
        List<PublicMovimentacaoDTO> movimentacoes,
        List<PublicDocumentoDTO> documentos,
        List<String> capabilityCodes,
        List<String> allowedScopes,
        List<String> warnings,
        List<ProfessionalForensicPanelLinkDto> routes
) {
}
