package com.tcc.pjb.backend.ai.juridica.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AjuizamentoIntentEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarClassificacaoParaSuporteDedicado() {
        Constructor<?> constructor = Arrays.stream(AjuizamentoIntentEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(4);
        assertThat(parameterTypes).contains(AjuizamentoIntentClassificationSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHeuristicasDeClassificacao() {
        Set<String> methodNames = Arrays.stream(AjuizamentoIntentEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "mapAreaToRamo",
                        "mapAmbitoToRamo",
                        "mapNaturezaToRamo",
                        "inferirSubRamoPenal",
                        "inferirSubRamoCivil",
                        "mapRitoPenal",
                        "mapRitoCivil",
                        "mapRitoTrabalhista"
                );
    }
}
