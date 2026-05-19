package com.tcc.pjb.backend.modules.prazos.api;

public interface PrazoProcessualPort {

    PrazoProcessualCalculoResult calcularPrazo(PrazoProcessualCalculoCommand command);

    PrazoDiaForenseResult analisarDiaForense(PrazoDiaForenseCommand command);
}
