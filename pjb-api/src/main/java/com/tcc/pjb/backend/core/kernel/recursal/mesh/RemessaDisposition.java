package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record RemessaDisposition(
        boolean externa,
        boolean mesmosAutos,
        boolean autuacaoDestino,
        boolean distribuicaoDestino) {

    public RemessaDisposition {
        if (mesmosAutos) {
            autuacaoDestino = false;
            distribuicaoDestino = false;
        }
    }

    public static RemessaDisposition internaMesmosAutos() {
        return new RemessaDisposition(false, true, false, false);
    }

    public static RemessaDisposition internaAutuacaoDependencia() {
        return new RemessaDisposition(false, false, true, true);
    }

    public static RemessaDisposition externaDistribuicaoMesmaNumeracao() {
        return new RemessaDisposition(true, false, false, true);
    }

    public static RemessaDisposition externaAutuacaoDistribuicao() {
        return new RemessaDisposition(true, false, true, true);
    }

    public boolean mesmaNumeracao() {
        return !autuacaoDestino;
    }

    public boolean autosApartadosDependencia() {
        return !externa && !mesmosAutos && autuacaoDestino;
    }

    public boolean remessaOutroGrau() {
        return externa;
    }

    public String name() {
        if (mesmosAutos) {
            return externa ? "REMESSA_EXTERNA_MESMOS_AUTOS" : "TRAMITACAO_INTERNA_MESMOS_AUTOS";
        }
        if (autosApartadosDependencia()) {
            return "AUTUACAO_DEPENDENCIA";
        }
        return externa ? "REMESSA_EXTERNA" : "TRAMITACAO_INTERNA";
    }
}
