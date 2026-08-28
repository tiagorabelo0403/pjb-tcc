package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.EquipeFiltroContextoQuery;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PjbRlsEquipeResolverTest {

    @Test
    void semContextoAtivo_retornaInactive() {
        EquipeFiltroContextoQuery query = mock(EquipeFiltroContextoQuery.class);
        when(query.currentOrEmpty()).thenReturn(Optional.empty());
        PjbRlsEquipeResolver resolver = new PjbRlsEquipeResolver(query);

        assertThat(resolver.currentOrInactive()).isEqualTo(PjbRlsEquipeResolver.INACTIVE);
    }

    @Test
    void comContextoAtivo_retornaUsuarioEEquipeComoTexto() {
        EquipeFiltroContextoQuery query = mock(EquipeFiltroContextoQuery.class);
        when(query.currentOrEmpty())
                .thenReturn(Optional.of(new EquipeFiltroContextoQuery.UsuarioEquipeAtivo(42L, 7L)));
        PjbRlsEquipeResolver resolver = new PjbRlsEquipeResolver(query);

        PjbRlsEquipeResolver.EquipeSettings settings = resolver.currentOrInactive();

        assertThat(settings.active()).isTrue();
        assertThat(settings.usuarioId()).isEqualTo("42");
        assertThat(settings.equipeId()).isEqualTo("7");
    }

    @Test
    void semEquipeAtiva_equipeIdParamMenosUm_ehPreservadoComoTexto() {
        EquipeFiltroContextoQuery query = mock(EquipeFiltroContextoQuery.class);
        when(query.currentOrEmpty())
                .thenReturn(Optional.of(new EquipeFiltroContextoQuery.UsuarioEquipeAtivo(42L, -1L)));
        PjbRlsEquipeResolver resolver = new PjbRlsEquipeResolver(query);

        assertThat(resolver.currentOrInactive().equipeId()).isEqualTo("-1");
    }
}
