package com.tcc.pjb.backend.modules.notificacoes.api;

public interface NotificacaoPrazoPort {

    NotificacaoPrazoDispatchResult publicarAlertaPrazo(NotificacaoPrazoCommand command);
}
