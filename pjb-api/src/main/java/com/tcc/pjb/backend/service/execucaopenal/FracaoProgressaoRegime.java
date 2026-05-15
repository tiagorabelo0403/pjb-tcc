package com.tcc.pjb.backend.service.execucaopenal;

public enum FracaoProgressaoRegime {

    DEZESSEIS_PORCENTO(16, "Não reincidente, sem violência/grave ameaça (LEP art. 112 I)"),
    VINTE_PORCENTO(20, "Não reincidente, com violência/grave ameaça (LEP art. 112 II)"),
    VINTE_CINCO_PORCENTO(25, "Reincidente, sem violência/grave ameaça (LEP art. 112 III)"),
    TRINTA_PORCENTO(30, "Reincidente, com violência/grave ameaça (LEP art. 112 IV)"),
    QUARENTA_PORCENTO(40, "Hediondo/equiparado, não reincidente específico, sem morte (LEP art. 112 V)"),
    CINQUENTA_PORCENTO(50, "Hediondo/equiparado, reincidente específico, sem morte (LEP art. 112 VI)"),
    SESSENTA_PORCENTO(60, "Hediondo/equiparado, com resultado morte, primário (LEP art. 112 VII)"),
    SETENTA_PORCENTO(70, "Hediondo/equiparado, com resultado morte, reincidente (LEP art. 112 VIII)");

    private final int percentual;
    private final String fundamentacao;

    FracaoProgressaoRegime(int percentual, String fundamentacao) {
        this.percentual = percentual;
        this.fundamentacao = fundamentacao;
    }

    public int percentual() { return percentual; }
    public String fundamentacao() { return fundamentacao; }

    public static FracaoProgressaoRegime resolver(
            boolean hediondo,
            boolean reincidenteEspecifico,
            boolean resultadoMorte,
            boolean reincidente,
            boolean comViolenciaOuGraveAmeaca) {
        if (hediondo) {
            if (resultadoMorte) return reincidente ? SETENTA_PORCENTO : SESSENTA_PORCENTO;
            return reincidenteEspecifico ? CINQUENTA_PORCENTO : QUARENTA_PORCENTO;
        }
        if (reincidente) return comViolenciaOuGraveAmeaca ? TRINTA_PORCENTO : VINTE_CINCO_PORCENTO;
        return comViolenciaOuGraveAmeaca ? VINTE_PORCENTO : DEZESSEIS_PORCENTO;
    }
}
