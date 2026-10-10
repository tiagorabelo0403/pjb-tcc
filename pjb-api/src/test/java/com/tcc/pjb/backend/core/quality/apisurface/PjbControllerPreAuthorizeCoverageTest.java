package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbControllerPreAuthorizeCoverageTest {

    @ArchTest
    static void todoEndpointDeclaraAutorizacaoNoMetodoOuNaClasse(JavaClasses classes) {
        List<JavaMethod> endpoints = classes.stream()
                .filter(classe -> classe.isMetaAnnotatedWith(Controller.class))
                .flatMap(classe -> classe.getMethods().stream())
                .filter(metodo -> metodo.isMetaAnnotatedWith(RequestMapping.class) || metodo.isAnnotatedWith(RequestMapping.class))
                .toList();

        assertThat(endpoints)
                .as("nenhum endpoint encontrado: a regra abaixo passaria sem verificar nada")
                .isNotEmpty();

        List<String> semAutorizacao = endpoints.stream()
                .filter(metodo -> !metodo.isAnnotatedWith(PreAuthorize.class) && !autorizaNaClasse(metodo.getOwner()))
                .map(metodo -> metodo.getOwner().getSimpleName() + "#" + metodo.getName())
                .sorted()
                .toList();

        assertThat(semAutorizacao).isEmpty();
    }

    private static boolean autorizaNaClasse(JavaClass classe) {
        return classe.isAnnotatedWith(PreAuthorize.class);
    }
}
