package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PjbGrantedAuthorityFactorySuporteTecnicoTest {

    @Test
    void suporteTecnicoRecebeRoleAutomaticaSemRegistroAdicional() {
        List<SimpleGrantedAuthority> authorities =
                PjbGrantedAuthorityFactory.authoritiesFor(TipoUsuario.SUPORTE_TECNICO, null);

        assertThat(authorities)
                .extracting(SimpleGrantedAuthority::getAuthority)
                .contains("ROLE_SUPORTE_TECNICO", "ROLE_USER");
    }
}
