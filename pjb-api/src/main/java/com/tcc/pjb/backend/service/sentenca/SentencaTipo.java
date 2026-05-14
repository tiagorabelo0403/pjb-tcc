package com.tcc.pjb.backend.service.sentenca;

public enum SentencaTipo {
    MERITO_PROCEDENTE,
    MERITO_IMPROCEDENTE,
    MERITO_PARCIALMENTE_PROCEDENTE,
    HOMOLOGATORIA_ACORDO,
    HOMOLOGATORIA_RECONHECIMENTO_PEDIDO,
    HOMOLOGATORIA_RENUNCIA,
    EXTINCAO_SEM_MERITO,
    LIMINAR_TUTELA_ANTECIPADA,
    ACORDAO_UNANIME,
    ACORDAO_MAIORIA,
    DECISAO_MONOCRATICA,
    DESPACHO_ORDINATORIO;

    public boolean admiteApelacao() {
        return switch (this) {
            case MERITO_PROCEDENTE, MERITO_IMPROCEDENTE, MERITO_PARCIALMENTE_PROCEDENTE,
                 HOMOLOGATORIA_ACORDO, HOMOLOGATORIA_RECONHECIMENTO_PEDIDO,
                 HOMOLOGATORIA_RENUNCIA, EXTINCAO_SEM_MERITO -> true;
            default -> false;
        };
    }

    public boolean transitaEmJulgado() {
        return switch (this) {
            case MERITO_PROCEDENTE, MERITO_IMPROCEDENTE, MERITO_PARCIALMENTE_PROCEDENTE,
                 HOMOLOGATORIA_ACORDO, HOMOLOGATORIA_RECONHECIMENTO_PEDIDO,
                 HOMOLOGATORIA_RENUNCIA, EXTINCAO_SEM_MERITO,
                 ACORDAO_UNANIME, ACORDAO_MAIORIA -> true;
            default -> false;
        };
    }

    public boolean ehAcordao() {
        return this == ACORDAO_UNANIME || this == ACORDAO_MAIORIA;
    }

    public boolean ehExtincaoSemResolucaoMerito() {
        return this == EXTINCAO_SEM_MERITO;
    }

    public boolean produzCoisaJulgadaMaterial() {
        return switch (this) {
            case MERITO_PROCEDENTE, MERITO_IMPROCEDENTE, MERITO_PARCIALMENTE_PROCEDENTE,
                 HOMOLOGATORIA_ACORDO, HOMOLOGATORIA_RECONHECIMENTO_PEDIDO,
                 HOMOLOGATORIA_RENUNCIA, ACORDAO_UNANIME, ACORDAO_MAIORIA -> true;
            default -> false;
        };
    }
}
