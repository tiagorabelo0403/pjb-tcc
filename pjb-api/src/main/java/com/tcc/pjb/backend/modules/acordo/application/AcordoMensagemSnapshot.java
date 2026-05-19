package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemVisibilidade;
import java.time.Instant;

public record AcordoMensagemSnapshot(
        Long id,
        Long sessaoId,
        Long autorId,
        AcordoMensagemTipo tipo,
        String conteudo,
        boolean confidencial,
        AcordoMensagemVisibilidade visibilidade,
        Instant createdAt
) {
}
