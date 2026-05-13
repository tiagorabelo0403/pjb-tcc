package com.tcc.pjb.backend.model.entity.enums;

public enum NationalCommunicationRecipientKind {
    PESSOA_FISICA,
    PESSOA_JURIDICA,
    ADVOGADO_OAB,
    DEFENSOR_PUBLICO,
    MINISTERIO_PUBLICO,
    FAZENDA_PUBLICA,
    JUIZO_DEPRECADO,
    ADVOCACIA_PUBLICA,
    DELEGACIA_POLICIA,
    POLICIA_PENAL,
    UNIDADE_PRISIONAL,
    CONSELHO_TUTELAR,
    PERITO_JUDICIAL,
    CONTADORIA_JUDICIAL,
    EQUIPE_PSICOSSOCIAL,
    CEJUSC,
    CARTORIO_EXTRAJUDICIAL,
    ORGAO_TECNICO_CONVENIADO,
    ORGAO_JUDICIAL_EXTERNO;

    public boolean isPessoaOuRepresentante() {
        return switch (this) {
            case PESSOA_FISICA, PESSOA_JURIDICA, ADVOGADO_OAB -> true;
            default -> false;
        };
    }

    public boolean isInstitucional() {
        return !isPessoaOuRepresentante();
    }
}
