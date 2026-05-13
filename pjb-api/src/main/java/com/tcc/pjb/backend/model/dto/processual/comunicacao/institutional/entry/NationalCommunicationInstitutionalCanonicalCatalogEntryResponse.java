package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import java.util.List;

public record NationalCommunicationInstitutionalCanonicalCatalogEntryResponse(
        String atoCanonico,
        String destinatarioKind,
        String papelProcessual,
        String tipoComunicacao,
        boolean exigeCienciaPessoal,
        boolean bloqueiaMarcoProcessual,
        String gateCode,
        String canalPrincipalSugerido,
        List<String> fallbacksSugeridos,
        String fundamentoLegal,
        List<String> justificativas
) {
}
