package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

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

    @Test
    void declaracoesConflitantesQueOSpringSecurityRecusaSaoAcusadas() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(DeclaracaoConflitante.class))
                .containsExactly("DeclaracaoConflitante#conflito (declaracoes conflitantes)");
    }

    @Test
    void subclasseDeControllerAnotadoEhControllerParaOSpringEParaAVarredura() {
        JavaClasses sondas = new ClassFileImporter().importClasses(HerdaControllerAnotado.class, ControllerBaseAnotado.class);

        assertThat(PjbControllerPreAuthorizeCoverageTest.podeSerController(sondas.get(HerdaControllerAnotado.class))).isTrue();
        assertThat(PjbControllerPreAuthorizeCoverageTest.ehController(HerdaControllerAnotado.class)).isTrue();
        assertThat(PjbControllerPreAuthorizeCoverageTest.ehController(ControllerBaseAnotado.class)).isFalse();
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(HerdaControllerAnotado.class))
                .containsExactly("HerdaControllerAnotado#daBase");
    }

    @Test
    void implementacaoDeInterfaceAnotadaComoControllerEhCobrada() {
        JavaClasses sondas = new ClassFileImporter().importClasses(ImplementaInterfaceController.class, InterfaceController.class);

        assertThat(PjbControllerPreAuthorizeCoverageTest.podeSerController(sondas.get(ImplementaInterfaceController.class))).isTrue();
        assertThat(PjbControllerPreAuthorizeCoverageTest.ehController(ImplementaInterfaceController.class)).isTrue();
    }

    @Test
    void classeSemControllerNaHierarquiaFicaForaDaVarredura() {
        JavaClasses sondas = new ClassFileImporter().importClasses(BaseSemController.class);

        assertThat(PjbControllerPreAuthorizeCoverageTest.podeSerController(sondas.get(BaseSemController.class))).isFalse();
        assertThat(PjbControllerPreAuthorizeCoverageTest.ehController(BaseSemController.class)).isFalse();
    }

    @Test
    void endpointDeclaradoComHttpExchangeTambemEhCobrado() {
        assertThat(PjbControllerPreAuthorizeCoverageTest.endpointsSemAutorizacao(UsaHttpExchange.class))
                .containsExactly("UsaHttpExchange#porExchange");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @PreAuthorize("isAuthenticated()")
    @interface SomenteLogado {
    }

    static class DeclaracaoConflitante {
        @GetMapping("/conflito")
        @PreAuthorize("permitAll()")
        @SomenteLogado
        public String conflito() {
            return "";
        }
    }

    @RestController
    abstract static class ControllerBaseAnotado {
        @GetMapping("/da-base")
        public String daBase() {
            return "";
        }
    }

    static class HerdaControllerAnotado extends ControllerBaseAnotado {
    }

    @Controller
    interface InterfaceController {
    }

    static class ImplementaInterfaceController implements InterfaceController {
    }

    static class UsaHttpExchange {
        @GetExchange("/exchange")
        public String porExchange() {
            return "";
        }
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
