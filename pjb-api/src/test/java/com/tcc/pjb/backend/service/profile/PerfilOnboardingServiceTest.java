package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

class PerfilOnboardingServiceTest {

    private final PerfilOnboardingService service = new PerfilOnboardingService();

    @Test
    void bloqueiaPeritoSemRegistroProfissional() {
        Usuario usuario = Usuario.builder()
                .tipoUsuario(TipoUsuario.PERITO_MEDICO)
                .email("perito@pjb.local")
                .nome("Perito")
                .cpf("12345678901")
                .perfil(TipoUsuario.PERITO_MEDICO.name())
                .senha("{bcrypt}x")
                .build();

        var resumo = service.avaliar(usuario);

        assertThat(resumo.concluido()).isFalse();
        assertThat(resumo.pendenciasBloqueantes()).anyMatch(v -> v.contains("Registro profissional obrigatório"));
    }

    @Test
    void sinalizaPsicossocialSemEspecialidade() {
        Usuario usuario = Usuario.builder()
                .tipoUsuario(TipoUsuario.PSICOLOGO_JUDICIAL)
                .email("psico@pjb.local")
                .nome("Psico")
                .cpf("12345678902")
                .perfil(TipoUsuario.PSICOLOGO_JUDICIAL.name())
                .senha("{bcrypt}x")
                .registroProfissional("CRP-01")
                .especialidades(Set.of())
                .build();

        var resumo = service.avaliar(usuario);

        assertThat(resumo.concluido()).isTrue();
        assertThat(resumo.pendenciasInformativas()).anyMatch(v -> v.contains("psicossocial"));
    }

    @Test
    void cartorioSemComarcaFicaBloqueado() {
        Usuario usuario = Usuario.builder()
                .tipoUsuario(TipoUsuario.REGISTRADOR_IMOVEIS)
                .email("registro@pjb.local")
                .nome("Registro")
                .cpf("12345678903")
                .perfil(TipoUsuario.REGISTRADOR_IMOVEIS.name())
                .senha("{bcrypt}x")
                .build();

        var resumo = service.avaliar(usuario);

        assertThat(resumo.concluido()).isFalse();
        assertThat(resumo.pendenciasBloqueantes()).anyMatch(v -> v.contains("Serventia extrajudicial"));
    }
}
