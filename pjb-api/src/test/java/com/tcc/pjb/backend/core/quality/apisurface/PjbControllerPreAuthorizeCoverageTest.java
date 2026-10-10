package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbControllerPreAuthorizeCoverageTest {

    @ArchTest
    static void todoEndpointDeclaraAutorizacaoNoMetodoOuNaClasse(JavaClasses classes) {
        List<Class<?>> controllers = classes.stream()
                .filter(classe -> classe.isMetaAnnotatedWith(Controller.class))
                .filter(classe -> !classe.isInterface() && !classe.getModifiers().contains(JavaModifier.ABSTRACT))
                .<Class<?>>map(JavaClass::reflect)
                .toList();

        assertThat(controllers.stream().mapToLong(controller -> endpoints(controller).size()).sum())
                .as("nenhum endpoint encontrado: a regra abaixo passaria sem verificar nada")
                .isPositive();

        List<String> semAutorizacao = controllers.stream()
                .flatMap(controller -> endpointsSemAutorizacao(controller).stream())
                .sorted()
                .toList();

        assertThat(semAutorizacao).isEmpty();
    }

    static List<String> endpointsSemAutorizacao(Class<?> controller) {
        boolean autorizaNaClasse = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class) != null;
        return endpoints(controller).stream()
                .filter(metodo -> !autorizaNaClasse && AnnotatedElementUtils.findMergedAnnotation(metodo, PreAuthorize.class) == null)
                .map(metodo -> controller.getSimpleName() + "#" + metodo.getName())
                .toList();
    }

    static Set<Method> endpoints(Class<?> controller) {
        return MethodIntrospector.selectMethods(controller, (MethodIntrospector.MetadataLookup<RequestMapping>) metodo ->
                AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class)).keySet();
    }
}
