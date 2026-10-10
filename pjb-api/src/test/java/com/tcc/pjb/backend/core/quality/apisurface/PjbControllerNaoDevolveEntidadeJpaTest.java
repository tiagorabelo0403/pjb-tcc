package com.tcc.pjb.backend.core.quality.apisurface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbControllerNaoDevolveEntidadeJpaTest {

    private static final String PACOTE_DO_PROJETO = "com.tcc.pjb.";

    @ArchTest
    static void nenhumEndpointDevolveEntidadeJpaNoCorpoDaResposta(JavaClasses classes) {
        assertThat(classes.stream().filter(classe -> classe.isAnnotatedWith(Entity.class)).count())
                .as("sem entidades no grafo a busca seguinte nao acusaria nada")
                .isGreaterThan(100);

        List<Class<?>> controllers = ControllersDaAplicacao.concretos(classes);
        assertThat(controllers.stream().mapToLong(controller -> ControllersDaAplicacao.endpoints(controller).size()).sum())
                .as("nenhum endpoint encontrado: a regra abaixo passaria sem verificar nada")
                .isPositive();

        List<String> violacoes = controllers.stream()
                .flatMap(controller -> ControllersDaAplicacao.endpoints(controller).stream()
                        .flatMap(metodo -> entidadeNaResposta(metodo.getGenericReturnType())
                                .map(caminho -> controller.getSimpleName() + "#" + metodo.getName() + " -> " + caminho)
                                .stream()))
                .sorted()
                .toList();

        assertThat(violacoes)
                .as("endpoint cujo tipo de resposta carrega entidade JPA, direto, em colecao, em pagina ou dentro de um DTO. "
                        + "O contrato publico passa a depender do schema e a serializacao arrasta as associacoes; devolva um "
                        + "record de resposta com as associacoes achatadas em identificador")
                .isEmpty();
    }

    static Optional<String> entidadeNaResposta(Type tipoDeRetorno) {
        return procurar(tipoDeRetorno, "", new HashSet<>());
    }

    private static Optional<String> procurar(Type tipo, String caminho, Set<Class<?>> visitados) {
        for (Class<?> classe : classesDe(tipo)) {
            if (ehPersistente(classe)) {
                return Optional.of(caminho + classe.getSimpleName());
            }
            if (classe.getName().startsWith(PACOTE_DO_PROJETO) && !classe.isEnum() && visitados.add(classe)) {
                for (Field campo : camposDeInstancia(classe)) {
                    Optional<String> achado = procurar(campo.getGenericType(),
                            caminho + classe.getSimpleName() + "." + campo.getName() + ":", visitados);
                    if (achado.isPresent()) {
                        return achado;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean ehPersistente(Class<?> classe) {
        return classe.isAnnotationPresent(Entity.class)
                || classe.isAnnotationPresent(Embeddable.class)
                || classe.isAnnotationPresent(MappedSuperclass.class);
    }

    private static List<Field> camposDeInstancia(Class<?> classe) {
        List<Field> campos = new ArrayList<>();
        for (Class<?> atual = classe; atual != null && atual.getName().startsWith(PACOTE_DO_PROJETO); atual = atual.getSuperclass()) {
            for (Field campo : atual.getDeclaredFields()) {
                if (!Modifier.isStatic(campo.getModifiers())) {
                    campos.add(campo);
                }
            }
        }
        return campos;
    }

    private static List<Class<?>> classesDe(Type tipo) {
        List<Class<?>> classes = new ArrayList<>();
        acumular(tipo, classes);
        return classes;
    }

    private static void acumular(Type tipo, List<Class<?>> classes) {
        if (tipo instanceof Class<?> classe) {
            if (classe.isArray()) {
                acumular(classe.getComponentType(), classes);
            } else {
                classes.add(classe);
            }
        } else if (tipo instanceof ParameterizedType parametrizado) {
            acumular(parametrizado.getRawType(), classes);
            for (Type argumento : parametrizado.getActualTypeArguments()) {
                acumular(argumento, classes);
            }
        } else if (tipo instanceof WildcardType curinga) {
            for (Type limite : curinga.getUpperBounds()) {
                acumular(limite, classes);
            }
        } else if (tipo instanceof GenericArrayType arrayGenerico) {
            acumular(arrayGenerico.getGenericComponentType(), classes);
        }
    }
}
