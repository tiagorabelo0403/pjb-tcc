package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record ChainOfCustodySyncEventResponse(
        String chaveCustodia,
        String digestColecaoSha256,
        String direcao,
        String operacao,
        String parceiroInstitucional,
        String noOrigem,
        String nonce,
        boolean integridadeOk,
        boolean assinaturaOk,
        boolean correspondenciaLocalOk,
        int totalEntradas,
        Instant ocorridoEm,
        String payloadDigestSha256
) {
}
