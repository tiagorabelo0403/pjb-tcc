package com.tcc.pjb.backend.core.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CertificadoIdentidadeResolverTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final CertificadoIdentidadeResolver resolver = new CertificadoIdentidadeResolver(usuarioRepository);

    @Test
    void cpfValidoUsuarioAtivoResolveIdentidade() {
        Usuario usuario = usuarioAtivo(true);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        IdentidadeResolucao resolucao = resolver.resolver(profile("12345678901"));

        assertThat(resolucao).isInstanceOf(IdentidadeResolvida.class);
        assertThat(((IdentidadeResolvida) resolucao).usuario()).isSameAs(usuario);
    }

    @Test
    void cpfValidoUsuarioInexistenteNaoResolve() {
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.empty());

        IdentidadeResolucao resolucao = resolver.resolver(profile("12345678901"));

        assertThat(resolucao).isEqualTo(new IdentidadeNaoResolvida(MotivoIdentidade.USUARIO_INEXISTENTE));
    }

    @Test
    void cpfValidoUsuarioInativoNaoResolve() {
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuarioAtivo(false)));

        IdentidadeResolucao resolucao = resolver.resolver(profile("12345678901"));

        assertThat(resolucao).isEqualTo(new IdentidadeNaoResolvida(MotivoIdentidade.USUARIO_INATIVO));
    }

    @Test
    void profileSemCpfNaoResolve() {
        IdentidadeResolucao resolucao = resolver.resolver(profile(null));

        assertThat(resolucao).isEqualTo(new IdentidadeNaoResolvida(MotivoIdentidade.CPF_AUSENTE));
    }

    @Test
    void profileNuloNaoResolve() {
        IdentidadeResolucao resolucao = resolver.resolver(null);

        assertThat(resolucao).isEqualTo(new IdentidadeNaoResolvida(MotivoIdentidade.CPF_AUSENTE));
    }

    @Test
    void cpfComMascaraNormalizaEResolve() {
        Usuario usuario = usuarioAtivo(true);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        IdentidadeResolucao resolucao = resolver.resolver(profile("123.456.789-01"));

        assertThat(resolucao).isInstanceOf(IdentidadeResolvida.class);
        assertThat(((IdentidadeResolvida) resolucao).usuario()).isSameAs(usuario);
    }

    private static IcpBrasilCertProfile profile(String cpf) {
        return new IcpBrasilCertProfile(
                "CN=Pessoa Teste",
                "CN=AC Teste",
                "ABC123",
                cpf,
                null,
                "Pessoa Teste",
                "A1",
                "AC TESTE",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"));
    }

    private static Usuario usuarioAtivo(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setAtivo(ativo);
        return usuario;
    }
}
