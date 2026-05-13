package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record PreventionDisposition(
        boolean obrigatoria,
        boolean mesmoRelator,
        boolean mesmoOrgaoFracionario,
        boolean mesmaTurmaOuCamara) {

    public static PreventionDisposition none() {
        return new PreventionDisposition(false, false, false, false);
    }

    public static PreventionDisposition strictSameRelator() {
        return new PreventionDisposition(true, true, true, true);
    }


    public String name() {
        return obrigatoria ? "PREVENTION_REQUIRED" : "PREVENTION_NONE";
    }
}
