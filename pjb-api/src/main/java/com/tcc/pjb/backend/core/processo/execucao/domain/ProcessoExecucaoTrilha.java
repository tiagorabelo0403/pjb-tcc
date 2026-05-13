package com.tcc.pjb.backend.core.processo.execucao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoExecucaoTrilha(
        String codigo,
        String titulo,
        String eixo,
        String fila,
        String inbox,
        String papelResponsavel,
        int prioridade,
        boolean bloqueante,
        long itensAbertosRelacionados,
        String modoPrincipal,
        String impacto,
        List<String> mandados,
        List<String> operacoesCustodia,
        List<String> guardas,
        List<String> fundamentos
) {
    public ProcessoExecucaoTrilha {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "EXECUCAO" : eixo;
        fila = fila == null ? "NAO_INFORMADA" : fila;
        inbox = inbox == null ? "NAO_INFORMADA" : inbox;
        papelResponsavel = papelResponsavel == null ? "NAO_INFORMADO" : papelResponsavel;
        prioridade = Math.max(0, prioridade);
        itensAbertosRelacionados = Math.max(0L, itensAbertosRelacionados);
        modoPrincipal = modoPrincipal == null ? "NAO_INFORMADO" : modoPrincipal;
        impacto = impacto == null ? "NAO_INFORMADO" : impacto;
        mandados = mandados == null ? List.of() : List.copyOf(mandados);
        operacoesCustodia = operacoesCustodia == null ? List.of() : List.copyOf(operacoesCustodia);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }


    public String code() {
        return codigo();
    }

    public String trailCode() {
        return codigo();
    }
}
