package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;
import org.junit.jupiter.api.AfterAll;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.OnlyIncludeTests.class)
class GrafoArchUnitEstaticoLiberadoGuardTest {

    @ArchTest
    static void campoEstaticoJavaClassesELiberadoNoAfterAll(JavaClasses testes) {
        List<JavaField> campos = testes.stream()
                .flatMap(classe -> classe.getFields().stream())
                .filter(campo -> campo.getModifiers().contains(JavaModifier.STATIC))
                .filter(campo -> campo.getRawType().isEquivalentTo(JavaClasses.class))
                .toList();

        assertThat(campos)
                .as("o guard precisa enxergar os grafos estaticos que existem hoje; lista vazia significa guard cego")
                .isNotEmpty();

        List<String> semLiberacao = campos.stream()
                .filter(campo -> campo.getAccessesToSelf().stream().noneMatch(GrafoArchUnitEstaticoLiberadoGuardTest::liberaNoAfterAll))
                .map(JavaField::getFullName)
                .toList();

        assertThat(semLiberacao)
                .as("grafo ArchUnit em campo estatico vive ate o fim da JVM de teste e ja derrubou a suite unitaria por OutOfMemoryError; "
                        + "atribua null ao campo num metodo @AfterAll")
                .isEmpty();
    }

    private static boolean liberaNoAfterAll(JavaFieldAccess acesso) {
        return acesso.getAccessType() == JavaFieldAccess.AccessType.SET
                && acesso.getOrigin().isAnnotatedWith(AfterAll.class);
    }
}
