package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class PeticaoInicialPeloCidadaoNaCadeiaTest {

    @Autowired
    private FilterChainProxy filterChainProxy;

    static Stream<Arguments> rotasDaPeticaoInicial() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/peticionamento/inicial/rascunhos/estruturar"),
                Arguments.of("POST", "/api/v1/peticionamento/inicial/rascunhos/salvar"),
                Arguments.of("GET", "/api/v1/peticionamento/inicial/rascunhos/minhas"),
                Arguments.of("GET", "/api/v1/peticionamento/inicial/rascunhos/7"),
                Arguments.of("POST", "/api/v1/peticionamento/inicial/rascunhos/7/protocolar")
        );
    }

    static Stream<Arguments> rotasProfissionaisDoPeticionamento() {
        return Stream.of(
                Arguments.of("POST", "/api/v1/peticionamento/inicial/sessao"),
                Arguments.of("POST", "/api/v1/peticionamento/studio/workspace"),
                Arguments.of("POST", "/api/v1/peticionamento/studio/minuta-rapida"),
                Arguments.of("GET", "/api/v1/advogado/cockpit/processos/7/custas")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("rotasDaPeticaoInicial")
    void cidadaoAutenticadoPassaPelaCadeiaNosRascunhosDaPeticaoInicial(String metodo, String rota) {
        assertThat(liberada(autenticado(TipoUsuario.CIDADAO), metodo, rota)).isTrue();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("rotasProfissionaisDoPeticionamento")
    void cidadaoContinuaBarradoNasRotasProfissionaisDoPeticionamento(String metodo, String rota) {
        assertThat(liberada(autenticado(TipoUsuario.CIDADAO), metodo, rota)).isFalse();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("rotasDaPeticaoInicial")
    void advogadoAutenticadoPassaPelaCadeiaNaPeticaoInicial(String metodo, String rota) {
        assertThat(liberada(autenticado(TipoUsuario.ADVOGADO), metodo, rota)).isTrue();
    }

    private static Authentication autenticado(TipoUsuario tipo) {
        return UsernamePasswordAuthenticationToken.authenticated("sonda-" + tipo.name(), null,
                PjbGrantedAuthorityFactory.authoritiesFor(tipo, null));
    }

    private boolean liberada(Authentication autenticacao, String metodo, String rota) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, rota);
        requisicao.setServletPath(rota);
        AuthorizationResult resultado = autorizacaoDaCadeia().authorize(() -> autenticacao, requisicao);
        return resultado != null && resultado.isGranted();
    }

    @SuppressWarnings("unchecked")
    private AuthorizationManager<HttpServletRequest> autorizacaoDaCadeia() {
        List<AuthorizationFilter> filtros = filterChainProxy.getFilterChains().stream()
                .flatMap(cadeia -> cadeia.getFilters().stream())
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .toList();
        assertThat(filtros).hasSize(1);
        return (AuthorizationManager<HttpServletRequest>) filtros.getFirst().getAuthorizationManager();
    }
}
