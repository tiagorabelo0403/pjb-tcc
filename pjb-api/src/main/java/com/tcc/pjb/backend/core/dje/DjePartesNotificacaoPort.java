package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;

public interface DjePartesNotificacaoPort {

    DjePartesNotificacaoResult notificar(DjePublicacao publicacao);
}
