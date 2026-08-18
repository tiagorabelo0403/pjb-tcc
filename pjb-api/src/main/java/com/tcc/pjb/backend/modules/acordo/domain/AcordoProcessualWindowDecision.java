package com.tcc.pjb.backend.modules.acordo.domain;

public record AcordoProcessualWindowDecision(
        boolean permitido,
        String motivo,
        AcordoTipoSala tipoSalaSugerido,
        boolean exigeSigilo,
        boolean exigeConciliadorMediador,
        boolean exigeAdvogado,
        boolean exigeDeterminacaoJudicial,
        AcordoMomentoProcessual momento
) {
    public static AcordoProcessualWindowDecision negado(String motivo) {
        return new AcordoProcessualWindowDecision(
                false,
                motivo,
                AcordoTipoSala.PROCESSUAL_CONTROLADA,
                false,
                false,
                false,
                false,
                AcordoMomentoProcessual.FORA_JANELA
        );
    }
}
