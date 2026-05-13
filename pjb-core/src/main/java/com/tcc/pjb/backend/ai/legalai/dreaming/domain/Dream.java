package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import java.time.Instant;

public record Dream(
        DreamId id,
        String anthropicDreamId,
        DreamStatus status,
        DreamInput input,
        DreamResult result,
        String instrucoes,
        String modelo,
        Instant criadoEm,
        Instant encerradoEm,
        String erro
) {

    private static final int MAX_INSTRUCOES_CHARS = 4096;

    public static Dream criar(DreamId id, DreamInput input, String instrucoes, String modelo, Instant agora) {
        if (instrucoes != null && instrucoes.length() > MAX_INSTRUCOES_CHARS) {
            throw new IllegalArgumentException("Instruções excedem limite de " + MAX_INSTRUCOES_CHARS + " caracteres");
        }
        return new Dream(id, null, DreamStatus.PENDING, input, null, instrucoes, modelo, agora, null, null);
    }

    public Dream comAnthropicId(String anthropicDreamId) {
        return new Dream(id, anthropicDreamId, DreamStatus.RUNNING, input, result, instrucoes, modelo, criadoEm, null, null);
    }

    public Dream completado(DreamResult dreamResult, Instant agora) {
        return new Dream(id, anthropicDreamId, DreamStatus.COMPLETED, input, dreamResult, instrucoes, modelo, criadoEm, agora, null);
    }

    public Dream falhou(String mensagemErro, Instant agora) {
        return new Dream(id, anthropicDreamId, DreamStatus.FAILED, input, null, instrucoes, modelo, criadoEm, agora, mensagemErro);
    }

    public Dream cancelado(Instant agora) {
        return new Dream(id, anthropicDreamId, DreamStatus.CANCELED, input, null, instrucoes, modelo, criadoEm, agora, null);
    }
}
