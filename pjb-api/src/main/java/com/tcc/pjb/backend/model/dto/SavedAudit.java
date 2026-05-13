package com.tcc.pjb.backend.model.dto;

public class SavedAudit {
    private final Object proposta;
    private final Object acordo;
    private final Object pdf;

    public SavedAudit(Object proposta, Object acordo, Object pdf) {
        this.proposta = proposta;
        this.acordo = acordo;
        this.pdf = pdf;
    }

    public Object getProposta() { return proposta; }
    public Object getAcordo() { return acordo; }
    public Object getPdf() { return pdf; }
}
