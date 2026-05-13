package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import java.util.List;

public record NationalCommunicationCanonicalActResolveResponse(
        String atoCanonico,
        Integer score,
        String destinatarioKind,
        String papelProcessual,
        String tipoComunicacao,
        Boolean exigeCienciaPessoal,
        Boolean bloqueiaFluxo,
        String gateCode,
        String fundamentoLegal,
        String hashResolucao,
        List<String> justificativas) {
}
