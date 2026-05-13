package com.tcc.pjb.backend.service.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.security.persona.PersonaKey;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

class PerfilCapabilityMatrixServiceTest {

    @Test
    void deveGerarTrilhasMagistraturaPorRito() {
        PerfilCapabilityMatrixService service = new PerfilCapabilityMatrixService();
        UserPersona persona = new UserPersona(TipoUsuario.JUIZ_FEDERAL, PersonaKey.JUIZ_FEDERAL, "Juiz Federal", "Vossa Excelência", GrauJurisdicao.PRIMEIRO_GRAU, EsferaJurisdicao.JUSTICA_FEDERAL, false);
        var trilhas = service.trilhasOperacionaisPorRito(persona, usuario());
        assertFalse(trilhas.isEmpty());
        assertTrue(service.widgetsMagistratura(usuario(), persona, false).contains("TRILHAS_POR_RITO"));
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNome("Juiz Federal");
        usuario.setEmail("juiz@pjb.test");
        usuario.setCpf("12345678909");
        usuario.setSenha("x");
        usuario.setTipoUsuario(TipoUsuario.JUIZ_FEDERAL);
        usuario.syncPerfilETipoUsuario();
        return usuario;
    }
}
