package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.BackendApplication;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class SecurityFilterChainPerfilTesteTest {

    private static final String PACOTE_PJB = "com.tcc.pjb.";
    private static final String NOME_DA_CADEIA = "springSecurityFilterChain";

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Test
    void cadeiaDeSegurancaDoPerfilDeTesteTemExatamenteEstaOrdem() {
        assertThat(filtrosDaCadeia()).containsExactly(
                "DisableEncodeUrlFilter",
                "WebAsyncManagerIntegrationFilter",
                "SecurityContextHolderFilter",
                "HeaderWriterFilter",
                "CorsFilter",
                "LogoutFilter",
                "ForwardedHeaderGuardFilter",
                "PasskeyAuthenticationFilter",
                "ApiSurfaceProtectionFilter",
                "MagistraturaIdleLockFilter",
                "ApiLoadSheddingFilter",
                "MagistraturaGeofenceFilter",
                "DecisionStepUpFilter",
                "ApiDatabasePressureShieldFilter",
                "DecisionClientBindingFilter",
                "PjbFunctionalAvailabilityFilter",
                "PjbOperationalAdmissionFilter",
                "PerimeterSecurityFilter",
                "ApiSecurityHardeningFilter",
                "ApiRouteGovernanceFilter",
                "SecurityContextHolderAwareRequestFilter",
                "AnonymousAuthenticationFilter",
                "SessionManagementFilter",
                "ExceptionTranslationFilter",
                "AuthorizationFilter",
                "InstitutionalCriticalActionHttpGuardFilter");
    }

    @Test
    void registroNoServletDosFiltrosDaCadeiaFicaDepoisDaCadeiaDeSeguranca() {
        ServletContextInitializerBeans iniciadores = new ServletContextInitializerBeans(beanFactory);
        int ordemDaCadeia = iniciadores.stream()
                .filter(DelegatingFilterProxyRegistrationBean.class::isInstance)
                .map(DelegatingFilterProxyRegistrationBean.class::cast)
                .filter(registro -> NOME_DA_CADEIA.equals(registro.getFilterName()))
                .mapToInt(DelegatingFilterProxyRegistrationBean::getOrder)
                .findFirst()
                .orElseThrow();
        Set<Filter> filtrosPjb = cadeiaUnica().getFilters().stream()
                .filter(filtro -> filtro.getClass().getName().startsWith(PACOTE_PJB))
                .collect(Collectors.toSet());
        List<FilterRegistrationBean<?>> registrosNoServlet = iniciadores.stream()
                .filter(FilterRegistrationBean.class::isInstance)
                .<FilterRegistrationBean<?>>map(FilterRegistrationBean.class::cast)
                .filter(registro -> filtrosPjb.contains(registro.getFilter()))
                .toList();

        assertThat(filtrosPjb)
                .as("a cadeia precisa conter filtros do PJB; conjunto vazio significa que a verificacao nao olhou nada")
                .isNotEmpty();

        assertThat(registrosNoServlet)
                .as("o Spring Boot registra no container de servlet cada filtro do PJB que e bean")
                .hasSameSizeAs(filtrosPjb);

        assertThat(registrosNoServlet)
                .as("o registro no servlet precisa vir depois da cadeia de seguranca (ordem %d), senao o filtro roda "
                        + "antes dela e e pulado dentro dela como ja executado", ordemDaCadeia)
                .allMatch(registro -> registro.getOrder() > ordemDaCadeia);

        assertThat(filtrosPjb)
                .as("so a marca de OncePerRequestFilter impede que o registro no servlet execute o filtro de novo")
                .allMatch(OncePerRequestFilter.class::isInstance);
    }

    private List<String> filtrosDaCadeia() {
        return cadeiaUnica().getFilters().stream()
                .map(filtro -> filtro.getClass().getSimpleName())
                .toList();
    }

    private SecurityFilterChain cadeiaUnica() {
        assertThat(filterChainProxy.getFilterChains()).hasSize(1);
        return filterChainProxy.getFilterChains().get(0);
    }
}
