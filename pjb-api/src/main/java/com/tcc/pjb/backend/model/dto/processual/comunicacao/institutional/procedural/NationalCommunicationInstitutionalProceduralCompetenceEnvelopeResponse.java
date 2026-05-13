package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProceduralCompetenceEnvelopeResponse(
        String eixoMaterial,
        String eixoProcedimental,
        String eixoFasico,
        String eixoAtuacao,
        boolean exigeAssinaturaForte,
        boolean exigeSegregacaoTitular,
        boolean bloqueiaAtosPosArquivamento,
        List<String> fundamentos
) {
}
