package com.tcc.pjb.backend.modules.acordo.api;

public record AcordoProcessualChatMessageResult(
        boolean espelhadaNaSala,
        Long sessaoId,
        Long mensagemSalaId,
        String statusSala,
        String confidencialidadeNivel,
        String motivo
) {
    public static AcordoProcessualChatMessageResult ignorada(String motivo) {
        return new AcordoProcessualChatMessageResult(false, null, null, null, null, motivo);
    }
}
