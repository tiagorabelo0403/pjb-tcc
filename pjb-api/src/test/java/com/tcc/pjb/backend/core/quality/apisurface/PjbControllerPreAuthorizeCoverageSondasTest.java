package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class PjbControllerPreAuthorizeCoverageSondasTest {

    @Test
    void endpointHerdadoDeSuperclasseSemAutorizacaoEhAcusado() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(HerdaEndpoint.class))
                .containsExactly("HerdaEndpoint#herdado");
    }

    @Test
    void endpointMapeadoNaInterfaceSemAutorizacaoEhAcusado() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(ImplementaContrato.class))
                .containsExactly("ImplementaContrato#doContrato");
    }

    @Test
    void autorizacaoDeclaradaNoMetodoDaInterfaceValeParaAImplementacao() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(ImplementaContratoAutorizado.class))
                .isEmpty();
    }

    @Test
    void anotacaoCompostaQueCarregaPreAuthorizeContaComoAutorizacao() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(UsaAnotacaoComposta.class))
                .isEmpty();
    }

    @Test
    void autorizacaoNaClasseCobreTodosOsEndpoints() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(AutorizaNaClasse.class))
                .isEmpty();
    }

    @Test
    void mapeamentoSoNaClasseNaoTransformaMetodoAuxiliarEmEndpoint() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpoints(MapeamentoSoNaClasse.class))
                .extracting(Method::getName)
                .containsExactly("rota");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @PreAuthorize("isAuthenticated()")
    @interface SomenteLogado {
    }

    static class BaseSemController {
        @GetMapping("/herdado")
        public String herdado() {
            return "";
        }
    }

    static class HerdaEndpoint extends BaseSemController {
        @GetMapping("/proprio")
        @PreAuthorize("isAuthenticated()")
        public String proprio() {
            return "";
        }
    }

    interface Contrato {
        @GetMapping("/contrato")
        String doContrato();
    }

    static class ImplementaContrato implements Contrato {
        @Override
        public String doContrato() {
            return "";
        }
    }

    interface ContratoAutorizado {
        @GetMapping("/contrato-autorizado")
        @PreAuthorize("isAuthenticated()")
        String doContrato();
    }

    static class ImplementaContratoAutorizado implements ContratoAutorizado {
        @Override
        public String doContrato() {
            return "";
        }
    }

    static class UsaAnotacaoComposta {
        @GetMapping("/composta")
        @SomenteLogado
        public String composta() {
            return "";
        }
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    static class AutorizaNaClasse {
        @GetMapping("/um")
        public String um() {
            return "";
        }

        @GetMapping("/dois")
        public String dois() {
            return "";
        }
    }

    @RequestMapping("/base")
    @PreAuthorize("isAuthenticated()")
    static class MapeamentoSoNaClasse {
        @GetMapping("/rota")
        public String rota() {
            return auxiliar();
        }

        public String auxiliar() {
            return "";
        }
    }
}
