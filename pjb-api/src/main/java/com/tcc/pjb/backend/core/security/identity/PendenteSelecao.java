package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import java.util.List;
import java.util.Objects;

public record PendenteSelecao(List<LotacaoInstituicao> lotacoesAtivas) implements ContextoResolucao {

    public PendenteSelecao {
        lotacoesAtivas = List.copyOf(Objects.requireNonNull(lotacoesAtivas));
    }
}
