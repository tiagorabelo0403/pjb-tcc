package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record DiligenceCheckpointResponse(
        String actor,
        String canal,
        String diligenciaReferencia,
        String checkpointTipo,
        double destinoLatitude,
        double destinoLongitude,
        double observadaLatitude,
        double observadaLongitude,
        double distanciaMetros,
        double raioMetros,
        boolean dentroDaCerca,
        String classificacao,
        String fonte,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Integer tentativaSequencia,
        String assinaturaLocalizacaoSha256,
        Instant capturadoEm,
        Instant registradoEm
) {
}
