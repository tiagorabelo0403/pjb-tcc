package com.tcc.pjb.backend.core.security.device.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AccountFreezeFilterTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final UserSecurityProfileRepository profileRepo = mock(UserSecurityProfileRepository.class);
    private final AccountFreezeFilter filter = new AccountFreezeFilter(currentUserService, profileRepo, new ObjectMapper());
    private final AtomicBoolean seguiuAdiante = new AtomicBoolean();

    @BeforeEach
    void usuarioLogado() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(currentUserService.currentUser()).thenReturn(Optional.of(usuario));
    }

    private MockHttpServletResponse executar(String caminho) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", caminho), response, (req, res) -> seguiuAdiante.set(true));
        return response;
    }

    private static UserSecurityProfile congeladoAte(LocalDateTime ate) {
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setFrozenAt(LocalDateTime.now().minusMinutes(5));
        profile.setFrozenUntil(ate);
        return profile;
    }

    @Test
    void contaCongeladaPeloBotaoDePanicoEhBarradaNasRotasAutenticadas() throws Exception {
        when(profileRepo.findByUsuarioId(7L)).thenReturn(Optional.of(congeladoAte(null)));

        MockHttpServletResponse response = executar("/api/v1/processos/1");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("account_frozen");
        assertThat(seguiuAdiante).isFalse();
    }

    @Test
    void congelamentoTemporarioVencidoLiberaAConta() throws Exception {
        when(profileRepo.findByUsuarioId(7L)).thenReturn(Optional.of(congeladoAte(LocalDateTime.now().minusMinutes(1))));

        executar("/api/v1/processos/1");

        assertThat(seguiuAdiante).isTrue();
    }

    @Test
    void contaSemPerfilDeSegurancaSegueNormalmente() throws Exception {
        when(profileRepo.findByUsuarioId(7L)).thenReturn(Optional.empty());

        executar("/api/v1/processos/1");

        assertThat(seguiuAdiante).isTrue();
    }

    @Test
    void falhaAoConsultarOCongelamentoNaoDeixaARequisicaoPassar() {
        when(profileRepo.findByUsuarioId(7L)).thenThrow(new IllegalStateException("banco indisponivel"));

        assertThatThrownBy(() -> executar("/api/v1/processos/1")).isInstanceOf(IllegalStateException.class);
        assertThat(seguiuAdiante).isFalse();
    }

    @Test
    void contaCongeladaConsegueConsultarOProprioEstadoDePanico() throws Exception {
        when(profileRepo.findByUsuarioId(7L)).thenReturn(Optional.of(congeladoAte(null)));

        executar("/api/v1/security/panic/status");

        assertThat(seguiuAdiante).isTrue();
    }

    @Test
    void rotasDeAutenticacaoContinuamAcessiveisParaContaCongelada() throws Exception {
        when(profileRepo.findByUsuarioId(7L)).thenReturn(Optional.of(congeladoAte(null)));

        executar("/api/v1/auth/govbr/login/iniciar");

        assertThat(seguiuAdiante).isTrue();
    }
}
