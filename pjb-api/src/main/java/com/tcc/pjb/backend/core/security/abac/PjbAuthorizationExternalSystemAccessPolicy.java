package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

final class PjbAuthorizationExternalSystemAccessPolicy {

    boolean canConsultBnmp(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        return tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL;
    }

    boolean canConsultCnib(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        return tipo == TipoUsuario.OFICIAL_JUSTICA
                || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR
                || (tipo != null && tipo.isCartorioExtrajudicial())
                || tipo == TipoUsuario.LEILOEIRO_JUDICIAL;
    }

    boolean canConsultRenajud(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        return tipo == TipoUsuario.DELEGADO_POLICIA
                || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || tipo == TipoUsuario.OFICIAL_JUSTICA
                || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
    }

    boolean canRequestInfojud(Usuario usuario, boolean delegatedOperation) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        if (tipo.isMagistratura()) {
            return true;
        }
        return delegatedOperation && tipo.isAssessor() && hasDelegationCredential();
    }

    boolean canRequestSisbajud(Usuario usuario, boolean delegatedOperation) {
        return canRequestInfojud(usuario, delegatedOperation);
    }

    boolean canRequestSerasajud(Usuario usuario, boolean delegatedOperation) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        if (tipo.isMagistratura()) {
            return true;
        }
        if (delegatedOperation && tipo.isAssessor()) {
            return hasDelegationCredential();
        }
        return tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria();
    }

    boolean canLocatePessoaByCpf(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return true;
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return true;
        }
        if (tipo.isMagistratura()) {
            return true;
        }
        return tipo.isAssessor() && hasDelegationCredential();
    }

    boolean canConsultarLocalizacaoSemProcesso(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        if (tipo.isMagistratura()) {
            return true;
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return true;
        }
        return tipo.isAssessor() && hasDelegationCredential();
    }

    boolean requiresFormalContextForStrictAddress(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return true;
        }
        if (tipo.isMagistratura()) {
            return false;
        }
        if (tipo.isAssessor()) {
            return !hasDelegationCredential();
        }
        return true;
    }

    boolean canAccessEnderecoEstrito(Usuario usuario, boolean possuiContextoFormal) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        if (tipo.isMagistratura()) {
            return true;
        }
        if (tipo.isAssessor()) {
            return hasDelegationCredential();
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return possuiContextoFormal;
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return possuiContextoFormal;
        }
        return false;
    }

    boolean canConsultarInteligenciaPatrimonial(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        return tipo.isMagistratura()
                || tipo == TipoUsuario.DELEGADO_POLICIA
                || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR
                || (tipo.isAssessor() && hasDelegationCredential());
    }

    boolean canConsultarMandadosPessoa(Usuario usuario) {
        TipoUsuario tipo = tipo(usuario);
        if (tipo == null) {
            return false;
        }
        return tipo.isMagistratura()
                || tipo == TipoUsuario.DELEGADO_POLICIA
                || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || (tipo.isAssessor() && hasDelegationCredential());
    }

    private TipoUsuario tipo(Usuario usuario) {
        return usuario != null ? usuario.getTipoUsuario() : null;
    }

    private boolean hasDelegationCredential() {
        return RequestContext.getDelegationCredential().isPresent();
    }
}
