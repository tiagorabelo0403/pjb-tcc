package com.tcc.pjb.backend.core.processo.sinalizacao.domain;

public record ProcessoSinalizacaoSeparador(
        String codigo,
        String titulo,
        int ordem,
        String motivo,
        boolean bloqueante
) {
    public ProcessoSinalizacaoSeparador {
        codigo = codigo == null || codigo.isBlank() ? "SEPARADOR" : codigo;
        titulo = titulo == null || titulo.isBlank() ? codigo : titulo;
        ordem = Math.max(0, ordem);
        motivo = motivo == null ? "" : motivo;
    }
}
