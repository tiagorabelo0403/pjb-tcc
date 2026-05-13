package com.tcc.pjb.backend.service.observabilidade;

public record AlertaOperacional(
        String nivel,
        String mensagem,
        String acaoSugerida
) {}
