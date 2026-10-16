package com.tcc.pjb.backend.configs.security.perimeter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestBodyHashFilter;
import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true",
                "pjb.security.perimeter.origin-governance.enabled=true"
        }
)
class HashDeCorpoNaCadeiaAntesDaGovernancaDeOrigemTest {

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    void cadeiaComGovernancaDeOrigemExecutaOHashDeCorpoAntesDela() {
        List<Class<?>> filtros = filterChainProxy.getFilterChains().stream()
                .map(cadeia -> cadeia.getFilters().stream().<Class<?>>map(Filter::getClass).toList())
                .filter(classes -> classes.contains(ApiRequestOriginGovernanceFilter.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nenhuma cadeia de seguranca com a governanca de origem"));

        assertThat(filtros).contains(RequestBodyHashFilter.class, ApiRequestOriginGovernanceFilter.class);
        assertThat(filtros.indexOf(RequestBodyHashFilter.class))
                .isLessThan(filtros.indexOf(ApiRequestOriginGovernanceFilter.class));
    }
}
