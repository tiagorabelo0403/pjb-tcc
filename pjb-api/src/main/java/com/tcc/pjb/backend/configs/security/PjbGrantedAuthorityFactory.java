package com.tcc.pjb.backend.configs.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public final class PjbGrantedAuthorityFactory {

    private PjbGrantedAuthorityFactory() {
    }

    public static List<SimpleGrantedAuthority> authoritiesFor(TipoUsuario tipoUsuario, EnteFederativo enteFederativo) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        roles.add("ROLE_USER");
        if (tipoUsuario == null) {
            addEnteRole(roles, enteFederativo);
            return roles.stream().map(SimpleGrantedAuthority::new).toList();
        }

        roles.add(roleOf(tipoUsuario.name()));
        expandTipoUsuario(roles, tipoUsuario);
        addEnteRole(roles, enteFederativo);
        return roles.stream().filter(Objects::nonNull).map(SimpleGrantedAuthority::new).toList();
    }

    private static void expandTipoUsuario(Set<String> roles, TipoUsuario tipo) {
        if (tipo.isAdmin()) {
            roles.add("ROLE_ADMIN");
        }
        if (tipo.isAdvocacia()) {
            roles.add("ROLE_ADVOCACIA");
            if (tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
                roles.add("ROLE_ADVOGADO");
            }
        }
        if (tipo.isMagistratura()) {
            roles.add("ROLE_MAGISTRATURA");
            roles.add("ROLE_MAGISTRADO");
            switch (tipo) {
                case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR -> roles.add("ROLE_JUIZ");
                case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> roles.add("ROLE_DESEMBARGADOR");
                case MINISTRO -> {
                    roles.add("ROLE_DESEMBARGADOR");
                    roles.add("ROLE_JUIZ");
                }
                default -> {
                }
            }
        }
        if (tipo.isAssessor()) {
            roles.add("ROLE_ASSESSOR");
            roles.add("ROLE_SERVIDOR_JUDICIARIO");
            if (tipo == TipoUsuario.ASSESSOR_DESEMBARGADOR || tipo == TipoUsuario.ASSESSOR_MINISTRO) {
                roles.add("ROLE_ASSESSOR_JUDICIAL");
            }
        }
        if (tipo.isMinisterioPublico()) {
            roles.add("ROLE_MINISTERIO_PUBLICO");
            roles.add("ROLE_MEMBRO_MINISTERIO_PUBLICO");
        }
        if (tipo.isDefensoriaPublica()) {
            roles.add("ROLE_DEFENSORIA_PUBLICA");
            roles.add("ROLE_DEFENSOR_PUBLICO");
        }
        if (tipo.isProcuradoria()) {
            roles.add("ROLE_PROCURADORIA");
            roles.add("ROLE_PROCURADOR");
        }
        if (tipo.isSegurancaPublica()) {
            roles.add("ROLE_SEGURANCA_PUBLICA");
            if (tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
                roles.add("ROLE_DELEGADO_POLICIA");
            }
        }
        if (tipo.isDelegadoOuAgente()) {
            roles.add("ROLE_OPERADOR_POLICIAL");
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            roles.add("ROLE_OFICIAL_JUSTICA");
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            roles.add("ROLE_CUMPRIMENTO_MANDADOS");
        }
        if (tipo.isPerito()) {
            roles.add("ROLE_PERITO");
            roles.add("ROLE_AUXILIAR_JUSTICA");
        }
        if (tipo == TipoUsuario.PSICOLOGO_JUDICIAL || tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL) {
            roles.add("ROLE_PSICOSSOCIAL_JUDICIAL");
        }
        if (tipo.isAuxiliarJustica()) {
            roles.add("ROLE_AUXILIAR_JUSTICA");
        }
        if (tipo.isConciliacaoMediacao()) {
            roles.add("ROLE_CONCILIACAO_MEDIACAO");
            switch (tipo) {
                case CONCILIADOR_CEJUSC -> roles.add("ROLE_CONCILIADOR_CEJUSC");
                case MEDIADOR -> roles.add("ROLE_MEDIADOR");
                case ARBITRO -> roles.add("ROLE_ARBITRO");
                default -> {
                }
            }
        }
        if (tipo.isCartorioExtrajudicial()) {
            roles.add("ROLE_CARTORIO_EXTRAJUDICIAL");
            if (tipo == TipoUsuario.REGISTRADOR_IMOVEIS || tipo == TipoUsuario.ESCREVENTE_CARTORIO) {
                roles.add("ROLE_TABELIAO");
            }
        }
        if (tipo.isServidorJudiciario()) {
            roles.add("ROLE_SERVIDOR_JUDICIARIO");
            roles.add("ROLE_SERVIDOR");
        }
        if (tipo == TipoUsuario.SERVIDOR_FORUM) {
            roles.add("ROLE_SERVIDOR_FORUM");
        }
        if (tipo.isSaude()) {
            roles.add("ROLE_SAUDE");
            switch (tipo) {
                case MEDICO, PERITO_MEDICO -> roles.add("ROLE_MEDICO");
                case HOSPITAL -> roles.add("ROLE_HOSPITAL");
                case UPA -> roles.add("ROLE_UPA");
                case CLINICA -> roles.add("ROLE_CLINICA");
                case PERITO_INSS -> roles.add("ROLE_PERITO_INSS");
                default -> {
                }
            }
            if (tipo == TipoUsuario.PERITO_MEDICO || tipo == TipoUsuario.PERITO_INSS) {
                roles.add("ROLE_PERITO");
                roles.add("ROLE_AUXILIAR_JUSTICA");
            }
        }
        if (tipo.isAcademico()) {
            roles.add("ROLE_ACADEMICO");
        }
    }

    private static void addEnteRole(Set<String> roles, EnteFederativo enteFederativo) {
        if (enteFederativo != null) {
            roles.add(roleOf(enteFederativo.name()));
        }
    }

    private static String roleOf(String value) {
        return value == null || value.isBlank() ? null : "ROLE_" + value;
    }
}
