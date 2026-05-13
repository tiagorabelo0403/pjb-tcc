package com.tcc.pjb.backend.model.entity.workspace;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public enum WorkspaceFilaAudience {
    ALL,
    MAGISTRATURA,
    ADVOCACIA,
    SERVIDOR,
    MP,
    DEFENSORIA,
    OFICIAL_JUSTICA,
    SEGURANCA,
    SAUDE,
    CIDADAO,
    ACADEMICO;

    public boolean applies(TipoUsuario tipo) {
        if (this == ALL) return true;
        if (tipo == null) return false;

        return switch (this) {
            case MAGISTRATURA -> tipo.isMagistratura();
            case ADVOCACIA -> tipo.isAdvocacia();
            case SERVIDOR -> tipo.isServidorJudiciario() || tipo.isAdministradorSistema();
            case MP -> tipo.isMinisterioPublico();
            case DEFENSORIA -> tipo.isDefensoriaPublica();
            case OFICIAL_JUSTICA -> tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
            case SEGURANCA -> tipo == TipoUsuario.DELEGADO_POLICIA
                    || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                    || tipo == TipoUsuario.AGENTE_POLICIAL
                    || tipo == TipoUsuario.ESCRIVAO_POLICIAL;
            case SAUDE -> tipo.isSaude();
            case CIDADAO -> tipo == TipoUsuario.CIDADAO;
            case ACADEMICO -> tipo.isAcademico();
            default -> false;
        };
    }
}
