package com.tcc.pjb.backend.platform.security.rbac;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Component
public class RbacRoleCatalog {

    private final Map<RbacRoleGroup, Set<String>> rolesByGroup;

    public RbacRoleCatalog() {
        EnumMap<RbacRoleGroup, Set<String>> map = new EnumMap<>(RbacRoleGroup.class);
        map.put(RbacRoleGroup.MAGISTRATURA, Set.of("ROLE_JUIZ", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL", "ROLE_JUIZ_ESPECIAL", "ROLE_JUIZ_ELEITORAL", "ROLE_JUIZ_TRABALHISTA", "ROLE_JUIZ_MILITAR", "ROLE_MAGISTRADO", "ROLE_DESEMBARGADOR", "ROLE_DESEMBARGADOR_FEDERAL", "ROLE_MINISTRO"));
        map.put(RbacRoleGroup.ASSESSORIA, Set.of("ROLE_ASSESSOR_JUDICIAL", "ROLE_ASSESSOR_DESEMBARGADOR", "ROLE_ASSESSOR_MINISTRO"));
        map.put(RbacRoleGroup.MINISTERIO_PUBLICO, Set.of("ROLE_MEMBRO_MINISTERIO_PUBLICO", "ROLE_PROMOTOR_ELEITORAL", "ROLE_PROMOTOR_TRABALHISTA", "ROLE_PROCURADOR_GERAL_REPUBLICA"));
        map.put(RbacRoleGroup.DEFENSORIA, Set.of("ROLE_DEFENSOR_PUBLICO", "ROLE_DEFENSOR_PUBLICO_FEDERAL"));
        map.put(RbacRoleGroup.PROCURADORIA, Set.of("ROLE_PROCURADOR", "ROLE_PROCURADORIA_MUNICIPAL", "ROLE_PROCURADORIA_ESTADUAL", "ROLE_PROCURADORIA_FEDERAL"));
        map.put(RbacRoleGroup.SEGURANCA_PUBLICA, Set.of("ROLE_DELEGADO_POLICIA", "ROLE_DELEGADO_POLICIA_FEDERAL", "ROLE_AGENTE_POLICIAL", "ROLE_ESCRIVAO_POLICIAL"));
        map.put(RbacRoleGroup.PERICIA, Set.of("ROLE_PERITO", "ROLE_PERITO_CRIMINAL", "ROLE_PERITO_AMBIENTAL", "ROLE_PERITO_CONTABIL", "ROLE_PERITO_ENGENHARIA", "ROLE_PERITO_DIGITAL", "ROLE_PERITO_INSS", "ROLE_PERITO_MEDICO", "ROLE_PSICOLOGO_JUDICIAL", "ROLE_ASSISTENTE_SOCIAL_JUDICIAL", "ROLE_ASSISTENTE_TECNICO"));
        map.put(RbacRoleGroup.OFICIALATO, Set.of("ROLE_OFICIAL_JUSTICA", "ROLE_OFICIAL_JUSTICA_AVALIADOR"));
        map.put(RbacRoleGroup.CONCILIACAO_MEDIACAO, Set.of("ROLE_CONCILIADOR_CEJUSC", "ROLE_MEDIADOR", "ROLE_ARBITRO"));
        map.put(RbacRoleGroup.AUXILIARES_JUSTICA, Set.of("ROLE_CONTADOR_JUDICIAL", "ROLE_ADMINISTRADOR_JUDICIAL", "ROLE_LEILOEIRO_JUDICIAL", "ROLE_CURADOR_ESPECIAL", "ROLE_CURADOR_AUSENTES", "ROLE_INVENTARIANTE"));
        map.put(RbacRoleGroup.CARTORIO_EXTRAJUDICIAL, Set.of("ROLE_TABELIAO", "ROLE_REGISTRADOR_IMOVEIS", "ROLE_ESCREVENTE_CARTORIO"));
        map.put(RbacRoleGroup.ADVOCACIA, Set.of("ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL"));
        map.put(RbacRoleGroup.SIGILO_ELEVADO, Set.of("ROLE_ADMIN", "ROLE_JUIZ", "ROLE_JUIZ_ESTADUAL", "ROLE_JUIZ_FEDERAL", "ROLE_JUIZ_ESPECIAL", "ROLE_JUIZ_ELEITORAL", "ROLE_JUIZ_TRABALHISTA", "ROLE_JUIZ_MILITAR", "ROLE_MAGISTRADO", "ROLE_DESEMBARGADOR", "ROLE_DESEMBARGADOR_FEDERAL", "ROLE_MINISTRO", "ROLE_MEMBRO_MINISTERIO_PUBLICO", "ROLE_PROMOTOR_ELEITORAL", "ROLE_PROMOTOR_TRABALHISTA", "ROLE_PROCURADOR_GERAL_REPUBLICA", "ROLE_DEFENSOR_PUBLICO", "ROLE_DEFENSOR_PUBLICO_FEDERAL", "ROLE_DELEGADO_POLICIA", "ROLE_DELEGADO_POLICIA_FEDERAL"));
        map.put(RbacRoleGroup.ADMINISTRACAO, Set.of("ROLE_ADMIN", "ROLE_ADMINISTRADOR"));
        map.put(RbacRoleGroup.CIDADANIA, Set.of("ROLE_CIDADAO"));
        map.put(RbacRoleGroup.SERVIDORIA, Set.of("ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM"));
        this.rolesByGroup = Map.copyOf(map);
    }

    public Set<String> rolesOf(RbacRoleGroup group) {
        return rolesByGroup.getOrDefault(group, Set.of());
    }

    public Set<String> resolveFor(TipoUsuario tipoUsuario) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (tipoUsuario == null) {
            return Set.of();
        }
        roles.add("ROLE_" + tipoUsuario.name());
        if (tipoUsuario.isAdmin()) {
            roles.addAll(rolesOf(RbacRoleGroup.ADMINISTRACAO));
        }
        if (tipoUsuario.isMagistratura()) {
            roles.addAll(rolesOf(RbacRoleGroup.MAGISTRATURA));
        }
        if (tipoUsuario.isAssessor()) {
            roles.addAll(rolesOf(RbacRoleGroup.ASSESSORIA));
        }
        if (tipoUsuario.isServidorJudiciario()) {
            roles.addAll(rolesOf(RbacRoleGroup.SERVIDORIA));
        }
        if (tipoUsuario.isAdvocacia()) {
            roles.addAll(rolesOf(RbacRoleGroup.ADVOCACIA));
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            roles.addAll(rolesOf(RbacRoleGroup.DEFENSORIA));
        }
        if (tipoUsuario.isProcuradoria()) {
            roles.addAll(rolesOf(RbacRoleGroup.PROCURADORIA));
        }
        if (tipoUsuario.isMinisterioPublico()) {
            roles.addAll(rolesOf(RbacRoleGroup.MINISTERIO_PUBLICO));
        }
        if (tipoUsuario.isSegurancaPublica()) {
            roles.addAll(rolesOf(RbacRoleGroup.SEGURANCA_PUBLICA));
        }
        if (tipoUsuario == TipoUsuario.OFICIAL_JUSTICA || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            roles.addAll(rolesOf(RbacRoleGroup.OFICIALATO));
        }
        if (tipoUsuario.isPerito()) {
            roles.addAll(rolesOf(RbacRoleGroup.PERICIA));
        }
        if (tipoUsuario.isConciliacaoMediacao()) {
            roles.addAll(rolesOf(RbacRoleGroup.CONCILIACAO_MEDIACAO));
        }
        if (tipoUsuario.isAuxiliarJustica()) {
            roles.addAll(rolesOf(RbacRoleGroup.AUXILIARES_JUSTICA));
        }
        if (tipoUsuario.isCartorioExtrajudicial()) {
            roles.addAll(rolesOf(RbacRoleGroup.CARTORIO_EXTRAJUDICIAL));
        }
        if (tipoUsuario == TipoUsuario.CIDADAO) {
            roles.addAll(rolesOf(RbacRoleGroup.CIDADANIA));
        }
        if (tipoUsuario.isPerfilCritico()) {
            roles.addAll(rolesOf(RbacRoleGroup.SIGILO_ELEVADO));
        }
        return Set.copyOf(roles);
    }
}
