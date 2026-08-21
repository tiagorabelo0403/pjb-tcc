package com.tcc.pjb.backend.model.entity.enums;

/**
 * Por qual meio a sessão (PasskeySession) foi emitida — os 4 fluxos de login do PJB
 * chamam PasskeySessionService.issue() com este valor, para que quem consumir a sessão
 * depois (ex.: exigência de certificado em fluxos sensíveis) saiba a origem real.
 */
public enum OrigemAutenticacaoSessao {
    PASSKEY,
    CERTIFICADO_ICP,
    GOVBR
}
