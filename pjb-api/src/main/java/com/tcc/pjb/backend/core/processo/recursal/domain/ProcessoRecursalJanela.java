package com.tcc.pjb.backend.core.processo.recursal.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoRecursalJanela(
        String codigo,
        String titulo,
        String eixo,
        boolean cabivel,
        boolean mesmosAutos,
        boolean exigeContrarrazoes,
        boolean preparoPotencial,
        boolean julgamentoColegiado,
        String rota,
        String tribunalDestino,
        String autoridadeAdmissibilidade,
        String autoridadeMerito,
        List<String> eventosIniciais,
        List<String> guardas,
        List<String> fundamentos
) {
    public ProcessoRecursalJanela {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "RECURSAL" : eixo;
        rota = rota == null ? "NAO_MAPEADA" : rota;
        tribunalDestino = tribunalDestino == null ? "NAO_INFORMADO" : tribunalDestino;
        autoridadeAdmissibilidade = autoridadeAdmissibilidade == null ? "NAO_INFORMADO" : autoridadeAdmissibilidade;
        autoridadeMerito = autoridadeMerito == null ? "NAO_INFORMADO" : autoridadeMerito;
        eventosIniciais = eventosIniciais == null ? List.of() : List.copyOf(eventosIniciais);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }


    public String code() {
        return codigo();
    }

    public String windowCode() {
        return codigo();
    }
}
