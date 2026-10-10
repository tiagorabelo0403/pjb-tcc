package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.SecurityAnnotationScanner;
import org.springframework.security.core.annotation.SecurityAnnotationScanners;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.service.annotation.HttpExchange;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbControllerPreAuthorizeCoverageTest {

    private static final SecurityAnnotationScanner<PreAuthorize> PRE_AUTHORIZE = SecurityAnnotationScanners.requireUnique(PreAuthorize.class);

    @ArchTest
    static void todoEndpointDeclaraAutorizacaoNoMetodoOuNaClasse(JavaClasses classes) {
        List<Class<?>> controllers = classes.stream()
                .filter(PjbControllerPreAuthorizeCoverageTest::podeSerController)
                .<Class<?>>map(JavaClass::reflect)
                .filter(PjbControllerPreAuthorizeCoverageTest::ehController)
                .toList();

        assertThat(controllers.stream().mapToLong(controller -> endpoints(controller).size()).sum())
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

    static boolean podeSerController(JavaClass classe) {
        return Stream.of(Stream.of(classe), classe.getAllRawSuperclasses().stream(), classe.getAllRawInterfaces().stream())
                .flatMap(tipos -> tipos)
                .anyMatch(tipo -> tipo.isMetaAnnotatedWith(Controller.class));
    }

    static boolean ehController(Class<?> classe) {
        return !classe.isInterface()
                && !Modifier.isAbstract(classe.getModifiers())
                && AnnotatedElementUtils.hasAnnotation(classe, Controller.class);
    }

    static List<String> endpointsSemAutorizacao(Class<?> controller) {
        return endpoints(controller).stream()
                .flatMap(metodo -> problemaDeAutorizacao(metodo, controller)
                        .map(problema -> controller.getSimpleName() + "#" + metodo.getName() + problema)
                        .stream())
                .toList();
    }

    static Set<Method> endpoints(Class<?> controller) {
        return MethodIntrospector.selectMethods(controller, (MethodIntrospector.MetadataLookup<Boolean>) metodo ->
                AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class)
                        || AnnotatedElementUtils.hasAnnotation(metodo, HttpExchange.class) ? Boolean.TRUE : null).keySet();
    }

    private static Optional<String> problemaDeAutorizacao(Method metodo, Class<?> controller) {
        try {
            return PRE_AUTHORIZE.scan(metodo, controller) == null ? Optional.of("") : Optional.empty();
        } catch (AnnotationConfigurationException conflito) {
            return Optional.of(" (declaracoes conflitantes)");
        }
    }
}
