package com.tcc.pjb.backend.core.processo.prazo.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProcessoPrazoMarco(
        String codigo,
        String titulo,
        String tipoPrazo,
        String eixo,
        LocalDate dataBase,
        LocalDate vencimento,
        int diasUteisProjetados,
        int diasCorridosProjetados,
        int diasRestantes,
        boolean vencido,
        boolean venceEmAteTresDias,
        boolean exigeCiencia,
        String efeitoProcessual,
        List<String> alertas,
        List<String> fundamentos
) {
    public ProcessoPrazoMarco {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(tipoPrazo);
        Objects.requireNonNull(eixo);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(vencimento);
        Objects.requireNonNull(efeitoProcessual);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
