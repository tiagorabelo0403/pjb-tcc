package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

class PjbGrantedAuthorityFactoryTest {

    @Test
    void juizFederalRecebePapeisHierarquicos() {
        var roles = PjbGrantedAuthorityFactory.authoritiesFor(TipoUsuario.JUIZ_FEDERAL, EnteFederativo.UNIAO)
                .stream()
                .map(a -> a.getAuthority())
                .toList();

        assertThat(roles)
                .contains("ROLE_JUIZ_FEDERAL", "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_MAGISTRATURA", "ROLE_UNIAO")
                .doesNotHaveDuplicates();
    }

    @Test
    void delegadoFederalHerdaRoleBaseDeDelegado() {
        var roles = PjbGrantedAuthorityFactory.authoritiesFor(TipoUsuario.DELEGADO_POLICIA_FEDERAL, null)
                .stream()
                .map(a -> a.getAuthority())
                .toList();

        assertThat(roles)
                .contains("ROLE_DELEGADO_POLICIA_FEDERAL", "ROLE_DELEGADO_POLICIA", "ROLE_SEGURANCA_PUBLICA", "ROLE_OPERADOR_POLICIAL");
    }

    @Test
    void servidorForumMantemPapelDeServidorJudiciario() {
        var roles = PjbGrantedAuthorityFactory.authoritiesFor(TipoUsuario.SERVIDOR_FORUM, null)
                .stream()
                .map(a -> a.getAuthority())
                .toList();

        assertThat(roles)
                .contains("ROLE_SERVIDOR_FORUM", "ROLE_SERVIDOR", "ROLE_SERVIDOR_JUDICIARIO");
    }
}
