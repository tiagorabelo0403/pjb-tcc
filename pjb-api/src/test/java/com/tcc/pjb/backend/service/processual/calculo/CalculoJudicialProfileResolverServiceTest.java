package com.tcc.pjb.backend.service.processual.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CalculoJudicialProfileResolverServiceTest {

    private final CalculoJudicialProfileResolverService service = new CalculoJudicialProfileResolverService();

    @Test
    void deveResolverContadorJudicialQuandoRoleContiverContador() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "contador",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CONTADOR_JUDICIAL"))
        );
        assertEquals(CalculoJudicialSolicitantePerfil.CONTADOR_JUDICIAL, service.resolve(authentication, null));
    }

    @Test
    void deveResolverTecnicoInstitucionalParaServidorForum() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "servidor",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_SERVIDOR_FORUM"))
        );
        assertEquals(CalculoJudicialSolicitantePerfil.TECNICO_INSTITUCIONAL, service.resolve(authentication, null));
    }

    @Test
    void deveAssumirCidadaoQuandoNaoHouverContextoAutenticado() {
        assertEquals(CalculoJudicialSolicitantePerfil.CIDADAO, service.resolve(null, null));
    }
}
