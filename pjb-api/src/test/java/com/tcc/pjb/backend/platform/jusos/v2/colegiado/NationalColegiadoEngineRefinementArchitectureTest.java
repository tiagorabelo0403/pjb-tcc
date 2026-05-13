package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class NationalColegiadoEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarTemasESessaoParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(NationalColegiadoEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(9);
        assertThat(parameterTypes)
                .contains(NationalColegiadoTemaSupport.class, NationalColegiadoSessionSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHelpersDeTemaEAnalyticsDeSessao() {
        Set<String> methodNames = Arrays.stream(NationalColegiadoEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "registrarTema",
                        "indexarTemaProcessos",
                        "desindexarTemaProcessos",
                        "compactarTemasRepetitivos",
                        "processosIndexados",
                        "mergeDistinct",
                        "marcoTema",
                        "montarEtiquetasSessao",
                        "montarChaveTema",
                        "inferirTribunalTema"
                );
    }
}
