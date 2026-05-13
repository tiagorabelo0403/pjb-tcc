package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record AdmissibilityDisposition(
        boolean juizoOrigem,
        RecursalAuthority autoridadeOrigem,
        boolean juizoDestino,
        RecursalAuthority autoridadeDestino,
        boolean admiteRetratacao,
        boolean admiteSobrestamento,
        boolean exigePrequestionamento,
        boolean exigeDemonstracaoRepercussaoGeral) {

    public AdmissibilityDisposition {
        if (!juizoOrigem) {
            autoridadeOrigem = null;
        }
        if (!juizoDestino) {
            autoridadeDestino = null;
        }
    }


    public String name() {
        return "ADMISSIBILITY";
    }
}
