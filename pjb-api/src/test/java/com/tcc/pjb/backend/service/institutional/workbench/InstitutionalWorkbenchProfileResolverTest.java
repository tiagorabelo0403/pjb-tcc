package com.tcc.pjb.backend.service.institutional.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalWorkbenchProfileResolverTest {

    private final InstitutionalWorkbenchProfileResolver resolver = new InstitutionalWorkbenchProfileResolver();

    @Test
    void shouldResolveFederalDefensoriaProfile() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL);
        usuario.setEnteFederativo(EnteFederativo.UNIAO);
        usuario.setUf("ce");
        usuario.setComarca("fortaleza");
        usuario.setEspecialidades(List.of("previdenciário", "saúde"));

        InstitutionalWorkbenchProfileResponse response = resolver.resolve(usuario);

        assertEquals("DEFENSORIA_FEDERAL", response.actorClass());
        assertEquals("DPU", response.institutionalBranch());
        assertEquals("UNIAO", response.federativeSphere());
        assertTrue(response.justiceMesh().contains("FEDERAL"));
        assertTrue(response.territorialAnchors().contains("CE"));
        assertTrue(response.specialties().contains("previdenciário"));
    }

    @Test
    void shouldResolveStateDelegateProfile() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setUf("sp");
        usuario.setComarca("campinas");

        InstitutionalWorkbenchProfileResponse response = resolver.resolve(usuario);

        assertEquals("DELEGACIA_ESTADUAL", response.actorClass());
        assertEquals("POLICIA_CIVIL_ESTADUAL", response.institutionalBranch());
        assertEquals("ESTADUAL", response.federativeSphere());
        assertTrue(response.capabilities().contains("INQUERITO"));
    }
}
