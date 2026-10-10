package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.SecurityAnnotationScanner;
import org.springframework.security.core.annotation.SecurityAnnotationScanners;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbControllerPreAuthorizeCoverageTest {

    private static final SecurityAnnotationScanner<PreAuthorize> PRE_AUTHORIZE = SecurityAnnotationScanners.requireUnique(PreAuthorize.class);

    @ArchTest
    static void todoEndpointDeclaraAutorizacaoNoMetodoOuNaClasse(JavaClasses classes) {
        List<Class<?>> controllers = ControllersDaAplicacao.concretos(classes);

        assertThat(controllers.stream().mapToLong(controller -> ControllersDaAplicacao.endpoints(controller).size()).sum())
                .as("nenhum endpoint encontrado: a regra abaixo passaria sem verificar nada")
                .isPositive();

        List<String> semAutorizacao = controllers.stream()
                .flatMap(controller -> endpointsSemAutorizacao(controller).stream())
                .sorted()
                .toList();

        assertThat(semAutorizacao)
                .as("endpoint sem @PreAuthorize, ou com declaracoes conflitantes que o Spring Security recusa em runtime")
                .isEmpty();
    }

    static List<String> endpointsSemAutorizacao(Class<?> controller) {
        return ControllersDaAplicacao.endpoints(controller).stream()
                .flatMap(metodo -> problemaDeAutorizacao(metodo, controller)
                        .map(problema -> controller.getSimpleName() + "#" + metodo.getName() + problema)
                        .stream())
                .toList();
    }

    private static Optional<String> problemaDeAutorizacao(Method metodo, Class<?> controller) {
        try {
            return PRE_AUTHORIZE.scan(metodo, controller) == null ? Optional.of("") : Optional.empty();
        } catch (AnnotationConfigurationException conflito) {
            return Optional.of(" (declaracoes conflitantes)");
        }
    }
}
