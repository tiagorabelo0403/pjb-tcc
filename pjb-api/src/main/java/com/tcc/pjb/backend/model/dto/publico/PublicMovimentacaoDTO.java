package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;

public record PublicMovimentacaoDTO(
        Long id,
        LocalDateTime data,
        String faseDe,
        String fasePara,
        String descricao
) {

    public Long getId() {
        return id();
    }

    public LocalDateTime getData() {
        return data();
    }

    public LocalDateTime getDataMovimentacao() {
        return data();
    }

    public String getFaseDe() {
        return faseDe();
    }

    public String getFasePara() {
        return fasePara();
    }

    public String getDescricao() {
        return descricao();
    }

    public LocalDateTime dataMovimentacao() {
        return data();
    }
}
