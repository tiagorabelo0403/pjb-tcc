package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record PreparoDisposition(
        boolean exigido,
        boolean dispensadoPorLeiOuRegimento,
        boolean complementacaoPermitida,
        boolean desercaoPossivel) {

    public PreparoDisposition {
        if (dispensadoPorLeiOuRegimento && exigido) {
            exigido = false;
        }
        if (!exigido) {
            complementacaoPermitida = false;
            desercaoPossivel = false;
        }
    }

    public static PreparoDisposition dispensado() {
        return new PreparoDisposition(false, true, false, false);
    }

    public static PreparoDisposition obrigatorio(boolean complementacaoPermitida) {
        return new PreparoDisposition(true, false, complementacaoPermitida, true);
    }
}
