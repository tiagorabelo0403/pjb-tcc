package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.BackendApplication;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class PermitAllDeclaradoLiberadoPelaCadeiaTest {

    private static final Authentication ANONIMO = new AnonymousAuthenticationToken(
            "sonda", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void rotaQueDeclaraPermitAllEhLiberadaPelaCadeiaParaQuemNaoEstaLogado() {
        AuthorizationManager<HttpServletRequest> autorizacaoDaCadeia = autorizacaoDaCadeia();
        List<Map.Entry<RequestMappingInfo, HandlerMethod>> declaramPermitAll = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(rota -> declaraPermitAll(rota.getValue()))
                .toList();

        assertThat(declaramPermitAll)
                .as("nenhuma rota com permitAll() encontrada: a regra abaixo passaria sem verificar nada")
                .isNotEmpty();

        List<String> barradas = declaramPermitAll.stream()
                .flatMap(rota -> requisicoesAnonimas(rota.getKey())
                        .filter(requisicao -> !liberada(autorizacaoDaCadeia, requisicao))
                        .map(requisicao -> requisicao.getMethod() + " " + requisicao.getRequestURI() + " -> "
                                + rota.getValue().getBeanType().getSimpleName() + "#" + rota.getValue().getMethod().getName()))
                .distinct()
                .sorted()
                .toList();

        assertThat(barradas)
                .as("rota anotada com permitAll() que a cadeia de seguranca barra para anonimo: a anotacao promete acesso "
                        + "publico que nunca acontece; libere a rota na cadeia ou declare a autorizacao que vale de fato")
                .isEmpty();
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

    private static boolean declaraPermitAll(HandlerMethod rota) {
        PreAuthorize doMetodo = AnnotatedElementUtils.findMergedAnnotation(rota.getMethod(), PreAuthorize.class);
        PreAuthorize efetiva = doMetodo != null ? doMetodo : AnnotatedElementUtils.findMergedAnnotation(rota.getBeanType(), PreAuthorize.class);
        return efetiva != null && efetiva.value().replace(" ", "").equals("permitAll()");
    }

    private static Stream<MockHttpServletRequest> requisicoesAnonimas(RequestMappingInfo info) {
        Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
        Stream<String> verbos = metodos.isEmpty() ? Stream.of("GET") : metodos.stream().map(RequestMethod::name);
        List<String> caminhos = info.getPatternValues().stream()
                .map(padrao -> padrao.replaceAll("\\{[^}]*}", "1").replace("**", "x").replace("*", "x"))
                .toList();
        return verbos.flatMap(verbo -> caminhos.stream().map(caminho -> {
            MockHttpServletRequest requisicao = new MockHttpServletRequest(verbo, caminho);
            requisicao.setServletPath(caminho);
            return requisicao;
        }));
    }

    private static boolean liberada(AuthorizationManager<HttpServletRequest> autorizacao, HttpServletRequest requisicao) {
        AuthorizationResult resultado = autorizacao.authorize(() -> ANONIMO, requisicao);
        return resultado != null && resultado.isGranted();
    }
}
