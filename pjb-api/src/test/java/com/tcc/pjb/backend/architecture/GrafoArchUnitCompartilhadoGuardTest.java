package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.CacheMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.OnlyIncludeTests.class)
class GrafoArchUnitCompartilhadoGuardTest {

    private static final String PACOTE_RAIZ = "com.tcc.pjb.backend";

    private static final Set<String> IMPORTACOES_EM_MASSA = Set.of(
            "importPackages", "importPackagesOf", "importClasspath", "importLocations",
            "importPath", "importPaths", "importJar", "importJars", "importUrl", "importUrls");

    @ArchTest
    static void classeComArchTestPrecisaDeAnalyzeClasses(JavaClasses testes) {
        List<JavaClass> comArchTest = testes.stream()
                .filter(GrafoArchUnitCompartilhadoGuardTest::declaraArchTest)
                .toList();

        assertThat(comArchTest)
                .as("o guard precisa enxergar as classes com @ArchTest; lista vazia significa guard cego")
                .isNotEmpty();

        List<String> semAnalyzeClasses = comArchTest.stream()
                .filter(classe -> !classe.isAnnotatedWith(AnalyzeClasses.class))
                .map(JavaClass::getName)
                .toList();

        assertThat(semAnalyzeClasses)
                .as("sem @AnalyzeClasses direto na classe a engine do ArchUnit 1.3 descarta a classe com um WARN e o build "
                        + "termina verde com zero testes executados; uma biblioteca de regras incluida por ArchTests.in(...) "
                        + "tambem cai aqui e exige ajustar esta regra de forma explicita")
                .isEmpty();
    }

    @ArchTest
    static void importDoCodigoDeProducaoUsaUmaUnicaChaveDeCache(JavaClasses testes) {
        List<JavaClass> importamPacoteRaiz = testes.stream()
                .filter(classe -> classe.isAnnotatedWith(AnalyzeClasses.class))
                .filter(classe -> importaOPacoteRaizInteiro(classe.getAnnotationOfType(AnalyzeClasses.class)))
                .toList();

        List<String> producaoCompartilhada = importamPacoteRaiz.stream()
                .filter(classe -> usaSomente(classe.getAnnotationOfType(AnalyzeClasses.class), ImportOption.DoNotIncludeTests.class))
                .map(JavaClass::getName)
                .toList();

        assertThat(producaoCompartilhada)
                .as("o guard precisa enxergar os testes que importam o codigo de producao inteiro; menos de dois significa "
                        + "que nao ha cache a compartilhar e a regra abaixo nao verifica nada")
                .hasSizeGreaterThanOrEqualTo(2);

        List<String> chavesParalelas = importamPacoteRaiz.stream()
                .filter(classe -> !usaSomente(classe.getAnnotationOfType(AnalyzeClasses.class), ImportOption.DoNotIncludeTests.class))
                .filter(classe -> !usaSomente(classe.getAnnotationOfType(AnalyzeClasses.class), ImportOption.OnlyIncludeTests.class))
                .map(JavaClass::getName)
                .toList();

        assertThat(chavesParalelas)
                .as("@AnalyzeClasses sobre %s com outra combinacao de opcoes cria uma segunda chave no cache do ArchUnit e "
                        + "importa o codigo de producao inteiro de novo; use packages = \"%s\", "
                        + "importOptions = ImportOption.DoNotIncludeTests.class e nenhum outro atributo", PACOTE_RAIZ, PACOTE_RAIZ)
                .isEmpty();
    }

    @ArchTest
    static void nenhumTesteImportaPacoteOuClasspathForaDoCacheDoArchUnit(JavaClasses testes) {
        List<String> importadoresAvulsos = testes.stream()
                .flatMap(classe -> Stream.<JavaCodeUnitAccess<?>>concat(
                        classe.getMethodCallsFromSelf().stream(), classe.getMethodReferencesFromSelf().stream()))
                .filter(chamada -> chamada.getTargetOwner().isEquivalentTo(ClassFileImporter.class))
                .filter(chamada -> IMPORTACOES_EM_MASSA.contains(chamada.getName()))
                .map(chamada -> chamada.getOriginOwner().getName() + "#" + chamada.getName())
                .distinct()
                .toList();

        assertThat(importadoresAvulsos)
                .as("ClassFileImporter importando pacote, classpath ou localizacao monta o grafo fora do cache compartilhado "
                        + "e repete a cada classe o custo que @AnalyzeClasses paga uma vez so por JVM; importClasses(...) "
                        + "de poucas classes segue permitido")
                .isEmpty();
    }

    @ArchTest
    static void nenhumTesteGuardaGrafoArchUnitEmCampoEstatico(JavaClasses testes) {
        List<String> campos = testes.stream()
                .flatMap(classe -> classe.getFields().stream())
                .filter(campo -> campo.getModifiers().contains(JavaModifier.STATIC))
                .filter(campo -> campo.getRawType().isEquivalentTo(JavaClasses.class))
                .map(JavaField::getFullName)
                .toList();

        assertThat(campos)
                .as("grafo ArchUnit em campo estatico vive ate o fim da JVM de teste e ja derrubou a suite unitaria por "
                        + "OutOfMemoryError; receba JavaClasses como parametro de um metodo @ArchTest")
                .isEmpty();
    }

    private static boolean declaraArchTest(JavaClass classe) {
        return classe.getFields().stream().anyMatch(campo -> campo.isAnnotatedWith(ArchTest.class))
                || classe.getMethods().stream().anyMatch(metodo -> metodo.isAnnotatedWith(ArchTest.class));
    }

    private static boolean importaOPacoteRaizInteiro(AnalyzeClasses analise) {
        return analise.wholeClasspath()
                || analise.locations().length > 0
                || Arrays.stream(analise.packages()).anyMatch(GrafoArchUnitCompartilhadoGuardTest::cobreOPacoteRaiz)
                || Arrays.stream(analise.packagesOf()).map(Class::getPackageName)
                        .anyMatch(GrafoArchUnitCompartilhadoGuardTest::cobreOPacoteRaiz);
    }

    private static boolean cobreOPacoteRaiz(String pacote) {
        return pacote.isEmpty() || PACOTE_RAIZ.equals(pacote) || PACOTE_RAIZ.startsWith(pacote + ".");
    }

    private static boolean usaSomente(AnalyzeClasses analise, Class<? extends ImportOption> opcao) {
        return analise.packagesOf().length == 0
                && analise.locations().length == 0
                && !analise.wholeClasspath()
                && analise.cacheMode() == CacheMode.FOREVER
                && Set.of(analise.packages()).equals(Set.of(PACOTE_RAIZ))
                && List.of(analise.importOptions()).equals(List.of(opcao));
    }
}
